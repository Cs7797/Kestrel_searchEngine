
package com.kestrel.datasets;

import com.kestrel.InvertedIndex;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SciDocsEvaluationTest {

    @Test
    void shouldEvaluateSciDocsSearchQuality() throws Exception {

        Path datasetPath = Path.of("datasets/scidocs");

        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();

        // 1. Load corpus and build index
        var documents = loader.load(
                datasetPath.resolve("corpus.jsonl"));

        InvertedIndex index = new InvertedIndex();

        documents.forEach(index::addDocument);

        var queries = loader.loadQueries(
                datasetPath.resolve("queries.jsonl"));

        var qrels = loader.loadQrels(
                datasetPath.resolve("qrels/test.tsv"));

        // 2. Select diagnostic query
        String targetQueryId =
                "de8e80d409aaaa3244da4f2cb5b5bb053d453cee";

        String query =
                "Detection of distributed denial of service attacks using machine learning algorithms in software defined networks";

        var relevantDocuments = qrels.get(targetQueryId);

        assertNotNull(relevantDocuments,
                "Relevant documents should exist for target query");

        System.out.println("\n========================================");
        System.out.println("       QUERY DIAGNOSTICS");
        System.out.println("========================================");

        System.out.println("Query: " +
                queries.stream()
                        .filter(q -> q.id().equals(targetQueryId))
                        .findFirst()
                        .orElseThrow()
                        .text());

        // 3. Inspect relevant documents
        System.out.println("\n===== RELEVANT DOCUMENT INSPECTION =====");

        for (var entry : relevantDocuments.entrySet()) {

            if (entry.getValue() <= 0) {
                continue;
            }

            String documentId = entry.getKey();

            var document = documents.stream()
                    .filter(d -> d.getId().equals(documentId))
                    .findFirst()
                    .orElseThrow();

            System.out.println("\nDocument ID: " + document.getId());
            System.out.println("Title: " + document.getTitle());

            System.out.println("Content: " +
                    document.getContent().substring(
                            0,
                            Math.min(300, document.getContent().length())));

            System.out.println("Relevance: " + entry.getValue());
        }

        // 4. Term frequency diagnostics
        System.out.println("\n===== TERM FREQUENCY DIAGNOSTICS =====");

        var queryTerms = List.of(
                "detection",
                "distributed",
                "denial",
                "service",
                "attacks",
                "machine",
                "learning",
                "algorithms",
                "software",
                "defined",
                "networks"
        );

        for (var entry : relevantDocuments.entrySet()) {

            if (entry.getValue() <= 0) {
                continue;
            }

            String documentId = entry.getKey();

            System.out.println("\nDocument: " + documentId);

            for (String term : queryTerms) {

                int tf = index.getTermFrequency(term, documentId);

                if (tf > 0) {
                    System.out.println(term + " = " + tf);
                }
            }
        }

        // 5. Relevant document ranking
        System.out.println("\n===== RELEVANT DOCUMENT RANKS =====");

        List<String> rankedResults = index.searchRanked(query);

        for (var entry : relevantDocuments.entrySet()) {

            if (entry.getValue() <= 0) {
                continue;
            }

            String documentId = entry.getKey();

            int rank = rankedResults.indexOf(documentId);

            if (rank >= 0) {
                System.out.println(
                        "Document: " + documentId +
                        " | Rank: " + (rank + 1));
            } else {
                System.out.println(
                        "Document: " + documentId +
                        " | NOT FOUND IN RANKED RESULTS");
            }
        }

        // 6. Corpus coverage diagnostics
        Set<String> corpusIds = new HashSet<>();

        for (var document : documents) {
            corpusIds.add(document.getId());
        }

        Set<String> relevantIds = new HashSet<>();

        for (var judgments : qrels.values()) {
            judgments.forEach((documentId, score) -> {
                if (score > 0) {
                    relevantIds.add(documentId);
                }
            });
        }

        Set<String> missingIds = new HashSet<>(relevantIds);
        missingIds.removeAll(corpusIds);

        System.out.println("\n===== CORPUS COVERAGE DIAGNOSTICS =====");

        System.out.println("Unique corpus IDs: " + corpusIds.size());
        System.out.println("Unique relevant IDs: " + relevantIds.size());

        System.out.println("Relevant IDs found: " +
                (relevantIds.size() - missingIds.size()));

        System.out.println("Relevant IDs missing: " +
                missingIds.size());

        if (!missingIds.isEmpty()) {
            System.out.println("Sample missing IDs: " +
                    missingIds.stream().limit(10).toList());
        }

        // 8. Full SciDocs evaluation
        SciDocsEvaluator evaluator = new SciDocsEvaluator();

        var detailedResult = evaluator.evaluateDetailed(
                index,
                queries,
                qrels,
                10
        );

        var result = detailedResult.summary();

        evaluator.printQueryReport(detailedResult);

        System.out.println("\n========================================");
        System.out.println("       KESTREL SEARCH QUALITY");
        System.out.println("========================================");

        System.out.println("Corpus documents: " + documents.size());
        System.out.println("Loaded queries: " + queries.size());

        System.out.println("Queries evaluated: " +
                result.evaluatedQueries());

        System.out.printf(
                "Precision@10: %.4f%n",
                result.precisionAtK());

        System.out.printf(
                "Recall@10:    %.4f%n",
                result.recallAtK());

        System.out.printf(
                "NDCG@10:      %.4f%n",
                result.ndcgAtK());

        // 9. Assertions
        assertEquals(25657, documents.size());
        assertEquals(1000, queries.size());
        assertTrue(result.evaluatedQueries() > 0);
    }
}
