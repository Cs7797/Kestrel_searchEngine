
package com.kestrel.datasets;

import com.kestrel.InvertedIndex;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SciDocsEvaluationTest {

    @Test
    void shouldEvaluateSciDocsSearchQuality() throws Exception {

        Path datasetPath = Path.of("datasets/scidocs");

        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();

        // Load corpus and build index
        var documents = loader.load(
                datasetPath.resolve("corpus.jsonl"));

        InvertedIndex index = new InvertedIndex();

        documents.forEach(index::addDocument);

        // Load queries and relevance judgments
        var queries = loader.loadQueries(
                datasetPath.resolve("queries.jsonl"));

        var qrels = loader.loadQrels(
                datasetPath.resolve("qrels/test.tsv"));

        // Evaluate search quality
        SciDocsEvaluator evaluator = new SciDocsEvaluator();

        var result = evaluator.evaluateDetailed(
                index,
                queries,
                qrels,
                10
        ).summary();

        // Print final evaluation results
        System.out.println("\n========================================");
        System.out.println("      KESTREL - SCIDOCS EVALUATION");
        System.out.println("========================================");

        System.out.println("Corpus documents: " + documents.size());
        System.out.println("Loaded queries: " + queries.size());
        System.out.println("Evaluated queries: " + result.evaluatedQueries());

        System.out.printf("Precision@10: %.4f%n",
                result.precisionAtK());

        System.out.printf("Recall@10:    %.4f%n",
                result.recallAtK());

        System.out.printf("NDCG@10:      %.4f%n",
                result.ndcgAtK());

        System.out.println("========================================");

        // Validate dataset and evaluation
        assertEquals(25657, documents.size());
        assertEquals(1000, queries.size());
        assertTrue(result.evaluatedQueries() > 0);
    }
}
