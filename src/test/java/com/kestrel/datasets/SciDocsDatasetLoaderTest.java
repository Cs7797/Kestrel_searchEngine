
package com.kestrel.datasets;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.kestrel.Document;
import com.kestrel.InvertedIndex;

import static org.junit.jupiter.api.Assertions.*;

class SciDocsDatasetLoaderTest {

    @Test
    void shouldLoadSciDocsCorpus() throws Exception {

        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();

        List<Document> documents = loader.load(
                Path.of("datasets/scidocs/corpus.jsonl"));

        assertEquals(25657, documents.size());

        assertNotNull(documents.get(0).getId());
        assertNotNull(documents.get(0).getTitle());
        assertNotNull(documents.get(0).getContent());

        System.out.println("Loaded documents: " + documents.size());
    }
    @Test
    void shouldIndexFullSciDocsCorpus() throws Exception {

        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();

        List<Document> documents = loader.load(
                Path.of("datasets/scidocs/corpus.jsonl"));

        InvertedIndex index = new InvertedIndex();

        long startTime = System.nanoTime();

        for (Document document : documents) {
            index.addDocument(document);
        }

        long endTime = System.nanoTime();

        double indexingTimeMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("Documents indexed: " + documents.size());
        System.out.printf("Indexing time: %.2f ms%n", indexingTimeMs);
        System.out.printf(
                "Indexing throughput: %.2f documents/sec%n",
                documents.size() / (indexingTimeMs / 1000.0));

        assertEquals(25657, documents.size());
    }

@Test
void shouldBenchmarkSciDocsSearch() throws Exception {

    SciDocsDatasetLoader loader = new SciDocsDatasetLoader();

    List<Document> documents = loader.load(
            Path.of("datasets/scidocs/corpus.jsonl")
    );

    InvertedIndex index = new InvertedIndex();

    for (Document document : documents) {
        index.addDocument(document);
    }

    List<String> queries = List.of(
            "machine learning neural networks",
            "information retrieval search engines",
            "database management systems",
            "computer vision deep learning",
            "natural language processing",
            "graph algorithms optimization",
            "distributed computing systems",
            "artificial intelligence",
            "software engineering",
            "data mining"
    );

    // Warm-up
    for (int i = 0; i < 5; i++) {
        for (String query : queries) {
            index.searchRanked(query);
        }
    }

    List<Double> latencies = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
        for (String query : queries) {

            long start = System.nanoTime();

            index.searchRanked(query);

            long end = System.nanoTime();

            latencies.add((end - start) / 1_000_000.0);
        }
    }

    Collections.sort(latencies);

    double total = latencies.stream()
            .mapToDouble(Double::doubleValue)
            .sum();

    double average = total / latencies.size();

    double median = latencies.get(latencies.size() / 2);

    double p95 = latencies.get(
            (int) Math.ceil(latencies.size() * 0.95) - 1
    );

    double p99 = latencies.get(
            (int) Math.ceil(latencies.size() * 0.99) - 1
    );

    System.out.println("\n===== KESTREL SEARCH BENCHMARK =====");
    System.out.println("Corpus size: " + documents.size());
    System.out.println("Total searches: " + latencies.size());
    System.out.printf("Average latency: %.4f ms%n", average);
    System.out.printf("Median latency: %.4f ms%n", median);
    System.out.printf("P95 latency: %.4f ms%n", p95);
    System.out.printf("P99 latency: %.4f ms%n", p99);
}

}
