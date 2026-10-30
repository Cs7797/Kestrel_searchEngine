
package com.kestrel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BenchmarkTest {

    @Test
    void benchmarkIndexingAndRankedSearch() {
        InvertedIndex index = new InvertedIndex();

        int documentCount = 10_000;

        // Measure indexing time.
        long indexingStart = System.nanoTime();

        for (int i = 0; i < documentCount; i++) {
            String content =
                    "java search engine indexing data structures";

            if (i % 100 == 0) {
                content += " kestrelrare";
            }

            Document document = new Document(
                    "D" + i,
                    "Document " + i,
                    content,
                    null
            );

            index.addDocument(document);
        }

        long indexingEnd = System.nanoTime();

        // Warm up ranked search before recording measurements.
        for (int i = 0; i < 10; i++) {
            index.searchRanked("kestrelrare");
        }

        // Collect repeated search measurements.
        List<Double> searchTimesMs = new ArrayList<>();
        int repetitions = 21;

        for (int i = 0; i < repetitions; i++) {
            long start = System.nanoTime();

            List<String> results =
                    index.searchRanked("kestrelrare");

            long end = System.nanoTime();

            assertEquals(100, results.size());

            double elapsedMs = (end - start) / 1_000_000.0;
            searchTimesMs.add(elapsedMs);
        }

        Collections.sort(searchTimesMs);

        double medianSearchMs =
                searchTimesMs.get(searchTimesMs.size() / 2);

        double indexingMs =
                (indexingEnd - indexingStart) / 1_000_000.0;

        System.out.printf(
                "%nKestrel benchmark (%d documents)%n"
                        + "Indexing time: %.2f ms%n"
                        + "Median ranked-search latency: %.2f ms%n"
                        + "Search repetitions: %d%n",
                documentCount,
                indexingMs,
                medianSearchMs,
                repetitions
        );
    }
}
