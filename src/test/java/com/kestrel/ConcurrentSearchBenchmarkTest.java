
package com.kestrel;

import com.kestrel.InvertedIndex;
import com.kestrel.datasets.SciDocsDatasetLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class ConcurrentSearchBenchmarkTest {

    private static final int[] THREAD_COUNTS = {1, 2, 4, 8};
    private static final int TOTAL_SEARCHES = 400;
    private static final int WARMUP_SEARCHES = 200;

    @Test
    void benchmarkConcurrentSearch() throws Exception {

        Path datasetPath = Path.of("datasets/scidocs");
        SciDocsDatasetLoader loader = new SciDocsDatasetLoader();

        // Build the index once, outside the timed section.
        var documents = loader.load(
                datasetPath.resolve("corpus.jsonl"));

        InvertedIndex index = new InvertedIndex();
        documents.forEach(index::addDocument);

        var queries = loader.loadQueries(
                datasetPath.resolve("queries.jsonl"))
                .stream()
                .map(q -> q.text())
                .filter(q -> q != null && !q.isBlank())
                .toList();

        if (queries.isEmpty()) {
            throw new IllegalStateException("No benchmark queries loaded");
        }

        // Warm up the JVM and search implementation.
        for (int i = 0; i < WARMUP_SEARCHES; i++) {
            index.searchRanked(queries.get(i % queries.size()));
        }

        System.out.println("\n==========================================");
        System.out.println("      KESTREL CONCURRENT BENCHMARK");
        System.out.println("==========================================");
        System.out.println("Documents: " + documents.size());
        System.out.println("Searches per configuration: " + TOTAL_SEARCHES);
        System.out.println();

        System.out.printf(
                "%-8s %-12s %-12s %-12s %-12s%n",
                "Threads", "QPS", "P50(ms)", "P95(ms)", "P99(ms)"
        );

        for (int threadCount : THREAD_COUNTS) {
            runBenchmark(index, queries, threadCount);
        }

        System.out.println("==========================================");
    }

    private void runBenchmark(
            InvertedIndex index,
            List<String> queries,
            int threadCount) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger nextTask = new AtomicInteger(0);

        // Each task writes its latency to a distinct array position.
        long[] latencies = new long[TOTAL_SEARCHES];

        try {
            List<Future<Void>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {

                futures.add(executor.submit(() -> {

                    start.await();

                    while (true) {
                        int taskId = nextTask.getAndIncrement();

                        if (taskId >= TOTAL_SEARCHES) {
                            break;
                        }

                        String query =
                                queries.get(taskId % queries.size());

                        long begin = System.nanoTime();

                        index.searchRanked(query);

                        long end = System.nanoTime();

                        latencies[taskId] = end - begin;
                    }

                    return null;
                }));
            }

            long benchmarkStart = System.nanoTime();

            // Release workers together.
            start.countDown();

            for (Future<Void> future : futures) {
                future.get(120, TimeUnit.SECONDS);
            }

            long benchmarkEnd = System.nanoTime();

            double elapsedSeconds =
                    (benchmarkEnd - benchmarkStart) / 1_000_000_000.0;

            double qps = TOTAL_SEARCHES / elapsedSeconds;

            Arrays.sort(latencies);

            double p50 = percentile(latencies, 50);
            double p95 = percentile(latencies, 95);
            double p99 = percentile(latencies, 99);

            System.out.printf(
                    "%-8d %-12.2f %-12.2f %-12.2f %-12.2f%n",
                    threadCount, qps, p50, p95, p99
            );

        } finally {
            executor.shutdownNow();
        }
    }

    private double percentile(long[] values, int percentile) {

        int index = (int) Math.ceil(
                percentile / 100.0 * values.length
        ) - 1;

        return values[Math.max(0, index)] / 1_000_000.0;
    }
}
