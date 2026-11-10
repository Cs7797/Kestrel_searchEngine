package com.kestrel.datasets;
import com.kestrel.InvertedIndex;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class NfcorpusEvaluationTest {
      @Test
    void evaluateBeir() throws Exception {

        Path path = Path.of("datasets/nfcorpus");

        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();

        var documents = loader.load(
                path.resolve("corpus.jsonl"));

        var queries = loader.loadQueries(
                path.resolve("queries.jsonl"));

        var qrels = loader.loadQrels(
                path.resolve("qrels/test.tsv"));

        // Build a fresh search index
        InvertedIndex index = new InvertedIndex();

        for (var document : documents) {
            index.addDocument(document);
        }

        // Evaluate ranking quality
        SciDocsEvaluator evaluator = new SciDocsEvaluator();

        var result = evaluator.evaluate(
                index, queries, qrels, 10);

        System.out.println("\n===== NFCORPUS RESULTS =====");
        System.out.println("Documents: " + documents.size());
        System.out.println("Evaluated queries: "
                + result.evaluatedQueries());

        System.out.printf("P@10: %.4f%n",
                result.precisionAtK());

        System.out.printf("R@10: %.4f%n",
                result.recallAtK());

        System.out.printf("NDCG@10: %.4f%n",
                result.ndcgAtK());
    }
}
