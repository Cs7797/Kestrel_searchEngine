package com.kestrel.datasets;

import com.kestrel.Document;
import com.kestrel.InvertedIndex;
import com.kestrel.TextNormalizer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SciDocsRetrievalDiagnosticsTest {

    private static final int K = 10;

    @Test
    void reportsWhySciDocsQueriesHaveNoRelevantResultsInTopTen() throws Exception {
        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();
        Path root = Path.of("datasets/scidocs");
        List<Document> documents = loader.load(root.resolve("corpus.jsonl"));
        List<SciDocsQuery> queries = loader.loadQueries(root.resolve("queries.jsonl"));
        Map<String, Map<String, Integer>> qrels = loader.loadQrels(root.resolve("qrels/test.tsv"));
        InvertedIndex index = new InvertedIndex();
        documents.forEach(index::addDocument);

        Map<String, Document> documentsById = documents.stream()
                .collect(Collectors.toMap(Document::getId, document -> document));
        Set<String> corpusIds = documentsById.keySet();
        CollectionStats stats = calculateCollectionStats(documents);

        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        long positivePairsMissingFromCorpus = 0;
        Map<Integer, Long> qrelGrades = qrels.values().stream()
                .flatMap(judgments -> judgments.values().stream())
                .collect(Collectors.groupingBy(score -> score, Collectors.counting()));

        for (SciDocsQuery query : queries) {
            Map<String, Integer> judgments = qrels.get(query.id());
            if (judgments == null || judgments.values().stream().noneMatch(score -> score > 0)) {
                continue;
            }

            List<String> queryTerms = TextNormalizer.normalizeQuery(query.text());
            Set<String> rawQueryTerms = new HashSet<>(TextNormalizer.tokenize(query.text()));
            Set<String> stemmedQueryTerms = TextNormalizer.tokenize(query.text()).stream()
                    .map(TextNormalizer::normalizeTerm)
                    .collect(Collectors.toSet());
            List<String> ranked = index.searchRanked(query.text());
            Map<String, Integer> rankById = new HashMap<>();
            for (int i = 0; i < ranked.size(); i++) {
                rankById.put(ranked.get(i), i + 1);
            }

            List<DocumentDiagnostic> relevant = new ArrayList<>();
            positivePairsMissingFromCorpus += judgments.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0 && !corpusIds.contains(entry.getKey())).count();
            for (Map.Entry<String, Integer> judgment : judgments.entrySet()) {
                if (judgment.getValue() <= 0) {
                    continue;
                }

                Document document = documentsById.get(judgment.getKey());
                if (document == null) {
                    continue;
                }

                Set<String> rawTitle = new HashSet<>(TextNormalizer.tokenize(document.getTitle()));
                Set<String> rawBody = new HashSet<>(TextNormalizer.tokenize(document.getContent()));
                Set<String> stemTitle = normalizedTerms(document.getTitle());
                Set<String> stemBody = normalizedTerms(document.getContent());
                Set<String> queryTitleOverlap = intersection(queryTerms, stemTitle);
                Set<String> queryBodyOverlap = intersection(queryTerms, stemBody);
                int rawOverlap = intersection(rawQueryTerms, union(rawTitle, rawBody)).size();
                int stemOverlap = intersection(stemmedQueryTerms, union(stemTitle, stemBody)).size();
                Set<String> normalizedOverlap = union(queryTitleOverlap, queryBodyOverlap);
                int rank = rankById.getOrDefault(document.getId(), -1);

                relevant.add(new DocumentDiagnostic(
                        document,
                        rawOverlap,
                        stemOverlap,
                        normalizedOverlap.size(),
                        queryTitleOverlap.size(),
                        queryBodyOverlap.size(),
                        rank,
                        rank > 0,
                        scoreQueryDocument(queryTerms, document, stats)
                ));
            }

            int positiveInTopK = relevant.stream()
                    .filter(doc -> doc.rank() > 0 && doc.rank() <= K)
                    .mapToInt(doc -> 1)
                    .sum();
            DocumentDiagnostic best = relevant.stream()
                    .filter(doc -> doc.rank() > 0)
                    .min(Comparator.comparingInt(DocumentDiagnostic::rank))
                    .orElseGet(() -> relevant.stream()
                            .max(Comparator.comparingInt(DocumentDiagnostic::normalizedOverlap))
                            .orElseThrow());

            int anyOverlap = (int) relevant.stream().filter(doc -> doc.normalizedOverlap() > 0).count();
            int candidateCount = (int) relevant.stream().filter(DocumentDiagnostic::candidate).count();
            int missingCandidates = (int) relevant.stream()
                    .filter(doc -> doc.normalizedOverlap() > 0 && !doc.candidate()).count();
            String category = classify(positiveInTopK, anyOverlap, missingCandidates, candidateCount, best);
            diagnostics.add(new QueryDiagnostic(
                    query, queryTerms, relevant, positiveInTopK, category, anyOverlap,
                    candidateCount, best, judgments,
                    ranked.stream().limit(K).toList()));
        }

        printAggregate(diagnostics, qrelGrades, documents, positivePairsMissingFromCorpus,
                queries, documentsById);
        printExamples(diagnostics, "d4820d5b290063cd13305fad39cc281705738e3e",
                "e00ab75a8aa637d9c4c5c020e9f2c54b31528031",
                "4649e3279edf653135f1f31da3e242ba451a93de");
        printDifficultQueries(diagnostics);
        assertEquals(1000, diagnostics.size());
        assertEquals(8, diagnostics.stream().filter(query -> query.category().equals("1")).count());
        assertEquals(492, diagnostics.stream().filter(query -> query.category().equals("3")).count());
        assertTrue(diagnostics.stream().allMatch(query -> query.category().equals("none")
                || query.category().equals("1")
                || query.category().equals("2")
                || query.category().equals("3")));
        assertEquals(0, diagnostics.stream().flatMap(query -> query.relevant().stream())
                .filter(doc -> doc.normalizedOverlap() > 0 && !doc.candidate()).count(),
                "A final-normalized query-term overlap must enter the current OR candidate set");
        assertEquals(0, diagnostics.stream().filter(query -> query.category().equals("2")).count());
    }

    private String classify(int positiveInTopK, int overlapDocs, int missingCandidates,
                            int candidates, DocumentDiagnostic best) {
        if (positiveInTopK > 0) {
            return "none";
        }
        if (overlapDocs == 0) {
            return "1";
        }
        if (missingCandidates > 0) {
            return "2";
        }
        if (candidates > 0 && best.rank() > K) {
            return "3";
        }
        return "1";
    }

    private CollectionStats calculateCollectionStats(List<Document> documents) {
        Map<String, Integer> titleDf = new HashMap<>();
        Map<String, Integer> bodyDf = new HashMap<>();
        long totalTitleLength = 0;
        long totalBodyLength = 0;

        for (Document document : documents) {
            Set<String> titleTerms = normalizedTerms(document.getTitle());
            Set<String> bodyTerms = normalizedTerms(document.getContent());
            titleTerms.forEach(term -> titleDf.merge(term, 1, Integer::sum));
            bodyTerms.forEach(term -> bodyDf.merge(term, 1, Integer::sum));
            totalTitleLength += TextNormalizer.searchableLength(document.getTitle());
            totalBodyLength += TextNormalizer.searchableLength(document.getContent());
        }

        return new CollectionStats(documents.size(), titleDf, bodyDf,
                (double) totalTitleLength / documents.size(),
                (double) totalBodyLength / documents.size());
    }

    private Set<String> normalizedTerms(String text) {
        return new HashSet<>(TextNormalizer.normalizeForIndex(text));
    }

    private Set<String> intersection(Set<String> first, Set<String> second) {
        Set<String> result = new HashSet<>(first);
        result.retainAll(second);
        return result;
    }

    private Set<String> intersection(List<String> first, Set<String> second) {
        return intersection(new HashSet<>(first), second);
    }

    private Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new HashSet<>(first);
        result.addAll(second);
        return result;
    }

    private double scoreQueryDocument(List<String> queryTerms, Document document, CollectionStats stats) {
        Map<String, Integer> titleTf = termFrequencies(document.getTitle());
        Map<String, Integer> bodyTf = termFrequencies(document.getContent());
        int titleLength = TextNormalizer.searchableLength(document.getTitle());
        int bodyLength = TextNormalizer.searchableLength(document.getContent());
        double score = 0.0;

        for (String term : queryTerms) {
            score += 2.0 * bm25(titleTf.getOrDefault(term, 0), stats.titleDf().getOrDefault(term, 0),
                    stats.documentCount(), titleLength, stats.averageTitleLength());
            score += bm25(bodyTf.getOrDefault(term, 0), stats.bodyDf().getOrDefault(term, 0),
                    stats.documentCount(), bodyLength, stats.averageBodyLength());
        }
        return score;
    }

    private Map<String, Integer> termFrequencies(String text) {
        Map<String, Integer> frequencies = new HashMap<>();
        TextNormalizer.normalizeForIndex(text).forEach(term -> frequencies.merge(term, 1, Integer::sum));
        return frequencies;
    }

    private double bm25(int tf, int df, int documentCount, int length, double averageLength) {
        if (tf == 0 || df == 0) {
            return 0.0;
        }
        double idf = Math.log(1.0 + (documentCount - df + 0.5) / (df + 0.5));
        double normalization = averageLength == 0.0 ? 1.0 : 1.0 - 0.75 + 0.75 * length / averageLength;
        return idf * (tf * 2.5) / (tf + 1.5 * normalization);
    }

    private void printAggregate(List<QueryDiagnostic> diagnostics, Map<Integer, Long> qrelGrades,
                                List<Document> documents, long positivePairsMissingFromCorpus,
                                List<SciDocsQuery> queries, Map<String, Document> documentsById) {
        Map<String, Long> categories = diagnostics.stream()
                .filter(query -> query.positiveInTopK() == 0)
                .collect(Collectors.groupingBy(QueryDiagnostic::category, Collectors.counting()));
        List<DocumentDiagnostic> allRelevant = diagnostics.stream()
                .flatMap(query -> query.relevant().stream()).toList();
        long noOverlap = allRelevant.stream().filter(doc -> doc.normalizedOverlap() == 0).count();
        long stemAddsOverlap = allRelevant.stream().filter(doc -> doc.stemOverlap() > doc.rawOverlap()).count();
        long stemRemovesOverlap = allRelevant.stream().filter(doc -> doc.stemOverlap() < doc.rawOverlap()).count();
        long stopwordRemovesOverlap = allRelevant.stream().filter(doc -> doc.normalizedOverlap() < doc.stemOverlap()).count();
        long stopwordAddsOverlap = allRelevant.stream().filter(doc -> doc.normalizedOverlap() > doc.stemOverlap()).count();
        long retrievedZeroGrades = diagnostics.stream()
                .flatMap(query -> query.rankedIds().stream().map(id -> Map.entry(query, id)))
                .filter(entry -> entry.getKey().judgments().getOrDefault(entry.getValue(), -1) == 0)
                .count();
        long queryIdsInCorpus = queries.stream().filter(query -> documentsById.containsKey(query.id())).count();
        long queryIdTitleMatches = queries.stream().filter(query -> {
            Document document = documentsById.get(query.id());
            return document != null && TextNormalizer.normalizeQuery(query.text())
                    .equals(TextNormalizer.normalizeQuery(document.getTitle()));
        }).count();
        long corpusBodiesStartingWithTitle = documents.stream().filter(document -> {
            List<String> titleTerms = TextNormalizer.normalizeForIndex(document.getTitle());
            List<String> bodyTerms = TextNormalizer.normalizeForIndex(document.getContent());
            return !titleTerms.isEmpty() && bodyTerms.size() >= titleTerms.size()
                    && bodyTerms.subList(0, titleTerms.size()).equals(titleTerms);
        }).count();
        double avgOverlap = allRelevant.stream().mapToInt(DocumentDiagnostic::normalizedOverlap)
                .average().orElse(0.0);
        double avgTitleOverlap = allRelevant.stream().mapToInt(DocumentDiagnostic::titleOverlap)
                .average().orElse(0.0);
        double avgBodyOverlap = allRelevant.stream().mapToInt(DocumentDiagnostic::bodyOverlap)
                .average().orElse(0.0);
        List<Integer> zeroQueryBestRanks = diagnostics.stream()
                .filter(query -> query.positiveInTopK() == 0 && query.best().rank() > 0)
                .map(query -> query.best().rank()).sorted().toList();
        int medianZeroQueryRank = zeroQueryBestRanks.get(zeroQueryBestRanks.size() / 2);
        int p90ZeroQueryRank = zeroQueryBestRanks.get((int) Math.ceil(0.90 * zeroQueryBestRanks.size()) - 1);
        List<Double> zeroQueryBestScores = diagnostics.stream()
                .filter(query -> query.positiveInTopK() == 0)
                .map(query -> query.best().score()).sorted().toList();
        double medianZeroQueryBestScore = zeroQueryBestScores.get(zeroQueryBestScores.size() / 2);

        System.out.println("\n===== SCIDOCS RETRIEVAL DIAGNOSTICS =====");
        System.out.printf("Corpus=%d; judged queries=%d; positive relevant document/query pairs in corpus=%d; positive pairs missing from corpus=%d%n",
                documents.size(), diagnostics.size(), allRelevant.size(), positivePairsMissingFromCorpus);
        System.out.printf("Preprocessing check: query IDs present in corpus=%d/%d; normalized query text equals same-ID corpus title=%d; corpus body begins with normalized title=%d/%d%n",
                queryIdsInCorpus, queries.size(), queryIdTitleMatches, corpusBodiesStartingWithTitle, documents.size());
        System.out.printf("Qrels grades=%s; normalized relevant-doc overlap=0 for %d/%d (%.1f%%)%n",
                qrelGrades, noOverlap, allRelevant.size(), 100.0 * noOverlap / allRelevant.size());
        System.out.printf("Zero-relevant-in-top10 queries=%d; query failure categories=%s%n",
                diagnostics.stream().filter(query -> query.positiveInTopK() == 0).count(), categories);
        System.out.printf("Relevant-doc pairs: stemming increased/decreased raw overlap=%d/%d; stopword removal increased/decreased stem overlap=%d/%d%n",
                stemAddsOverlap, stemRemovesOverlap, stopwordAddsOverlap, stopwordRemovesOverlap);
        System.out.printf("Mean matching query terms per positive relevant doc: all=%.2f, title=%.2f, body=%.2f%n",
                avgOverlap, avgTitleOverlap, avgBodyOverlap);
        System.out.printf("Zero-hit queries with a generated relevant candidate=%d; best relevant rank median/P90=%d/%d; best-relevant score median=%.4f%n",
                zeroQueryBestRanks.size(), medianZeroQueryRank, p90ZeroQueryRank, medianZeroQueryBestScore);
        System.out.printf("Final-normalized overlap but absent candidate=%d; positive relevant candidates=%d/%d (%.1f%%); retrieved qrel-grade-0 docs in top 10=%d%n",
                allRelevant.stream().filter(doc -> doc.normalizedOverlap() > 0 && !doc.candidate()).count(),
                allRelevant.stream().filter(DocumentDiagnostic::candidate).count(), allRelevant.size(),
                100.0 * allRelevant.stream().filter(DocumentDiagnostic::candidate).count() / allRelevant.size(),
                retrievedZeroGrades);
        System.out.println("Category 4 (positive qrel document retrieved but rejected by evaluator) is impossible here: evaluator relevance is qrel score > 0, and every such top-10 document is counted. Retrieved qrel score-0 documents are reported separately as judged nonrelevant.");
    }

    private void printExamples(List<QueryDiagnostic> diagnostics, String... queryIds) {
        System.out.println("\n===== THREE REQUESTED QUERIES =====");
        for (String queryId : queryIds) {
            diagnostics.stream().filter(query -> query.query().id().equals(queryId)).findFirst()
                    .ifPresent(this::printQueryDiagnostic);
        }
    }

    private void printDifficultQueries(List<QueryDiagnostic> diagnostics) {
        List<QueryDiagnostic> difficult = diagnostics.stream()
                .filter(query -> query.positiveInTopK() == 0)
                .sorted(Comparator.comparingInt((QueryDiagnostic query) -> query.best().rank() < 0
                                ? Integer.MAX_VALUE : query.best().rank()).reversed()
                        .thenComparing(query -> query.query().text()))
                .limit(20)
                .toList();
        System.out.println("\n===== 20 DIFFICULT QUERIES (ZERO POSITIVE TOP-10 RESULTS) =====");
        difficult.forEach(this::printQueryDiagnostic);
    }

    private void printQueryDiagnostic(QueryDiagnostic query) {
        DocumentDiagnostic best = query.best();
        System.out.printf("Q=%s | category=%s | text=%s%n", query.query().id(), query.category(), query.query().text());
        System.out.printf("  terms=%s | relevant=%d | overlap docs=%d | candidate relevant=%d | best rank=%d | best score=%.6f%n",
                query.terms(), query.relevant().size(), query.overlapDocs(), query.candidateDocs(), best.rank(), best.score());
        System.out.printf("  best doc=%s | title=%s | overlap raw/stem/Kestrel=%d/%d/%d title/body=%d/%d | candidate=%s%n",
                best.document().getId(), best.document().getTitle(), best.rawOverlap(), best.stemOverlap(),
                best.normalizedOverlap(), best.titleOverlap(), best.bodyOverlap(), best.candidate());
    }

    private record CollectionStats(int documentCount, Map<String, Integer> titleDf,
                                   Map<String, Integer> bodyDf,
                                   double averageTitleLength, double averageBodyLength) {}

    private record DocumentDiagnostic(Document document, int rawOverlap, int stemOverlap,
                                      int normalizedOverlap, int titleOverlap, int bodyOverlap,
                                      int rank, boolean candidate, double score) {}

    private record QueryDiagnostic(SciDocsQuery query, List<String> terms,
                                   List<DocumentDiagnostic> relevant, int positiveInTopK,
                                   String category, int overlapDocs, int candidateDocs,
                                   DocumentDiagnostic best, Map<String, Integer> judgments,
                                   List<String> rankedIds) {}
}
