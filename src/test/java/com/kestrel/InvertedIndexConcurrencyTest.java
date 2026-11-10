package com.kestrel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class InvertedIndexConcurrencyTest {

    @Test
    void concurrentSearchesReturnConsistentResults() throws Exception {

        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1", "Java Programming",
                "Java concurrency and multithreading", null));

        index.addDocument(new Document(
                "D2", "Python Programming",
                "Python programming fundamentals", null));

        int threadCount = 8;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<List<String>>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return index.search("java");
                }));
            }

            start.countDown();

            for (Future<List<String>> future : futures) {
                assertEquals(
                        List.of("D1"),
                        future.get(5, TimeUnit.SECONDS));
            }

        } finally {
            executor.shutdownNow();
        }
    }
    @Test
    void concurrentReadsAndWritesRemainConsistent() throws Exception {

        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1", "Java", "Java programming", null));

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<?>> futures = new ArrayList<>();

            // One writer repeatedly replaces D1.
            futures.add(executor.submit(() -> {
                start.await();

                for (int i = 0; i < 500; i++) {
                    String language = i % 2 == 0 ? "Python" : "Java";

                    index.addDocument(new Document(
                            "D1",
                            language,
                            language + " programming",
                            null));
                }

                return null;
            }));

            // Four concurrent readers.
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    start.await();

                    for (int j = 0; j < 500; j++) {
                        List<String> results = index.searchRanked("java");

                        // D1 can be present or absent,
                        // depending on the current version.
                        assertTrue(
                                results.isEmpty()
                                        || results.equals(List.of("D1")));

                        index.searchPhrase("python programming");
                        index.getTermFrequency("java", "D1");
                    }

                    return null;
                }));
            }

            start.countDown();

            for (Future<?> future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }

            // Writer's final iteration sets D1 to Java.
            assertEquals(List.of("D1"), index.search("java"));
            assertTrue(index.search("python").isEmpty());

        } finally {
            executor.shutdownNow();
        }
    }
@Test
void concurrentRebuildsNeverExposePartialIndex() throws Exception {

    InvertedIndex index = new InvertedIndex();

    List<Document> oldDocuments = List.of(
            new Document("D1", "Java", "Java programming", null),
            new Document("D2", "Java", "Java concurrency", null)
    );

    List<Document> newDocuments = List.of(
            new Document("D3", "Python", "Python programming", null),
            new Document("D4", "Python", "Python concurrency", null)
    );

    index.replaceAllDocuments(oldDocuments);

    ExecutorService executor = Executors.newFixedThreadPool(5);
    CountDownLatch start = new CountDownLatch(1);

    try {
        List<Future<?>> futures = new ArrayList<>();

        // One writer alternates between two complete indexes.
        futures.add(executor.submit(() -> {
            start.await();

            for (int i = 0; i < 100; i++) {
                index.replaceAllDocuments(
                        i % 2 == 0 ? newDocuments : oldDocuments
                );
            }

            return null;
        }));

        // Four readers repeatedly inspect the index.
        for (int i = 0; i < 4; i++) {
            futures.add(executor.submit(() -> {
                start.await();

                for (int j = 0; j < 100; j++) {
                    List<String> results = index.search("java");

                    assertTrue(
                            results.equals(List.of("D1", "D2"))
                                    || results.isEmpty(),
                            "Observed a partially rebuilt index: " + results
                    );
                }

                return null;
            }));
        }

        start.countDown();

        for (Future<?> future : futures) {
            future.get(15, TimeUnit.SECONDS);
        }

        // Final rebuild is oldDocuments (iteration 99).
        assertEquals(List.of("D1", "D2"), index.search("java"));

    } finally {
        executor.shutdownNow();
    }
}
}