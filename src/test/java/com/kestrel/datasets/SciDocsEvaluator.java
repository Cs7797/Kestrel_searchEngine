
package com.kestrel.datasets;

import com.kestrel.InvertedIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SciDocsEvaluator {

    public record EvaluationResult(
            int evaluatedQueries,
            double precisionAtK,
            double recallAtK,
            double ndcgAtK
    ) {}

    public EvaluationResult evaluate(
            InvertedIndex index,
            List<SciDocsQuery> queries,
            Map<String, Map<String, Integer>> qrels,
            int k
    ) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive");
        }

        double totalPrecision = 0.0;
        double totalRecall = 0.0;
        double totalNdcg = 0.0;
        int evaluatedQueries = 0;

        for (SciDocsQuery query : queries) {

            Map<String, Integer> relevance =
                    qrels.get(query.id());

            if (relevance == null || relevance.isEmpty()) {
                continue;
            }

            long totalRelevant = relevance.values()
                    .stream()
                    .filter(score -> score > 0)
                    .count();

            if (totalRelevant == 0) {
                continue;
            }

            List<String> results =
                    index.searchRanked(query.text());

            List<String> topK = results.stream()
                    .limit(k)
                    .toList();

            int relevantRetrieved = 0;

            for (String documentId : topK) {
                if (relevance.getOrDefault(documentId, 0) > 0) {
                    relevantRetrieved++;
                }
            }

            double precision =
                    (double) relevantRetrieved / k;

            double recall =
                    (double) relevantRetrieved / totalRelevant;

            double dcg = calculateDcg(topK, relevance);

            double idcg = calculateIdealDcg(relevance, k);

            double ndcg = idcg == 0.0 ? 0.0 : dcg / idcg;

            totalPrecision += precision;
            totalRecall += recall;
            totalNdcg += ndcg;

            evaluatedQueries++;
        }

        if (evaluatedQueries == 0) {
            throw new IllegalStateException(
                    "No queries with relevance judgments were evaluated"
            );
        }

        return new EvaluationResult(
                evaluatedQueries,
                totalPrecision / evaluatedQueries,
                totalRecall / evaluatedQueries,
                totalNdcg / evaluatedQueries
        );
    }

    private double calculateDcg(
            List<String> rankedDocuments,
            Map<String, Integer> relevance
    ) {
        double dcg = 0.0;

        for (int i = 0; i < rankedDocuments.size(); i++) {

            int score = relevance.getOrDefault(
                    rankedDocuments.get(i), 0
            );

            double gain = score;
            double discount = log2(i + 2);

            dcg += gain / discount;
        }

        return dcg;
    }

    private double calculateIdealDcg(
            Map<String, Integer> relevance,
            int k
    ) {
        List<Integer> idealScores = new ArrayList<>(
                relevance.values()
        );

        idealScores.sort(Comparator.reverseOrder());

        double idcg = 0.0;

        for (int i = 0; i < Math.min(k, idealScores.size()); i++) {

            int score = idealScores.get(i);

            if (score <= 0) {
                continue;
            }

           double gain = score;
            double discount = log2(i + 2);

            idcg += gain / discount;
        }

        return idcg;
    }

    private double log2(double value) {
        return Math.log(value) / Math.log(2);
    }
    public record DetailedEvaluationResult(
        EvaluationResult summary,
        List<QueryEvaluation> queryResults
) {}

public record QueryEvaluation(
        String queryId,
        String queryText,
        double precisionAtK,
        double recallAtK,
        double ndcgAtK,
        int relevantRetrieved,
        int totalRelevant,
        int resultsReturned,List<String> retrievedDocumentIds,
List<String> relevantDocumentIds) {
}

public DetailedEvaluationResult evaluateDetailed(
        InvertedIndex index,
        List<SciDocsQuery> queries,
        Map<String, Map<String, Integer>> qrels,
        int k
) {

    if (k <= 0) {
        throw new IllegalArgumentException("k must be positive");
    }

    List<QueryEvaluation> queryResults = new ArrayList<>();

    double totalPrecision = 0.0;
    double totalRecall = 0.0;
    double totalNdcg = 0.0;

    for (SciDocsQuery query : queries) {

        Map<String, Integer> relevance = qrels.get(query.id());

        if (relevance == null || relevance.isEmpty()) {
            continue;
        }

        int totalRelevant = (int) relevance.values()
                .stream()
                .filter(score -> score > 0)
                .count();

        if (totalRelevant == 0) {
            continue;
        }

        List<String> results = index.searchRanked(query.text());

        List<String> topK = results.stream()
                .limit(k)
                .toList();

        int relevantRetrieved = 0;

        for (String documentId : topK) {
            if (relevance.getOrDefault(documentId, 0) > 0) {
                relevantRetrieved++;
            }
        }

        double precision = (double) relevantRetrieved / k;
        double recall = (double) relevantRetrieved / totalRelevant;

        double dcg = calculateDcg(topK, relevance);
        double idcg = calculateIdealDcg(relevance, k);

        double ndcg = idcg == 0.0 ? 0.0 : dcg / idcg;

      queryResults.add(new QueryEvaluation(
        query.id(),
        query.text(),
        precision,
        recall,
        ndcg,
        relevantRetrieved,
        totalRelevant,
        topK.size(),
        topK,
        relevance.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList()
));

        totalPrecision += precision;
        totalRecall += recall;
        totalNdcg += ndcg;
    }

    if (queryResults.isEmpty()) {
        throw new IllegalStateException(
                "No queries with relevance judgments were evaluated"
        );
    }

    int count = queryResults.size();

    EvaluationResult summary = new EvaluationResult(
            count,
            totalPrecision / count,
            totalRecall / count,
            totalNdcg / count
    );

    return new DetailedEvaluationResult(summary, queryResults);
}

public void printQueryReport(DetailedEvaluationResult result) {

    List<QueryEvaluation> queryResults = result.queryResults();

    Comparator<QueryEvaluation> byNdcg =
            Comparator.comparingDouble(QueryEvaluation::ndcgAtK);

    List<QueryEvaluation> best = queryResults.stream()
            .sorted(byNdcg.reversed())
            .limit(20)
            .toList();

    List<QueryEvaluation> worst = queryResults.stream()
            .sorted(byNdcg)
            .limit(20)
            .toList();

    System.out.println("\n========== TOP 20 QUERIES ==========");

    printQueries(best);

    System.out.println("\n========== BOTTOM 20 QUERIES ==========");

    printQueries(worst);
}

private void printQueries(List<QueryEvaluation> queries) {

    for (QueryEvaluation query : queries) {

        System.out.printf(
                "ID: %s | Query: %s | P@10: %.3f | R@10: %.3f | NDCG@10: %.3f | Relevant: %d/%d | Returned: %d%n",
                query.queryId(),
                query.queryText(),
                query.precisionAtK(),
                query.recallAtK(),
                query.ndcgAtK(),
                query.relevantRetrieved(),
                query.totalRelevant(),
                query.resultsReturned()
        );
        if (query.ndcgAtK() == 0.0) {

    System.out.println("  Retrieved IDs: "
            + query.retrievedDocumentIds());

    System.out.println("  Relevant IDs: "
            + query.relevantDocumentIds());
}
    }
}

}
