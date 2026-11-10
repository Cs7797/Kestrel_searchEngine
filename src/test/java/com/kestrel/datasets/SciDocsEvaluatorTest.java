
package com.kestrel.datasets;

import com.kestrel.Document;
import com.kestrel.InvertedIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SciDocsEvaluatorTest {

    private final SciDocsEvaluator evaluator =
            new SciDocsEvaluator();

    @Test
    void shouldReturnPerfectMetricsForPerfectRanking() {

        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "doc-1",
                "Java Programming",
                "Java programming language",
                null
        ));

        List<SciDocsQuery> queries = List.of(
                new SciDocsQuery("q1", "java programming")
        );

        Map<String, Map<String, Integer>> qrels = Map.of(
                "q1",
                Map.of("doc-1", 1)
        );

        var result = evaluator.evaluate(
                index, queries, qrels, 1
        );

        assertEquals(1, result.evaluatedQueries());
        assertEquals(1.0, result.precisionAtK(), 0.0001);
        assertEquals(1.0, result.recallAtK(), 0.0001);
        assertEquals(1.0, result.ndcgAtK(), 0.0001);
    }

    @Test
    void shouldSkipQueriesWithoutRelevanceJudgments() {

        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "doc-1",
                "Java Programming",
                "Java programming language",
                null
        ));

        List<SciDocsQuery> queries = List.of(
                new SciDocsQuery("q1", "java"),
                new SciDocsQuery("q2", "programming")
        );

        Map<String, Map<String, Integer>> qrels = Map.of(
                "q1",
                Map.of("doc-1", 1)
        );

        var result = evaluator.evaluate(
                index, queries, qrels, 1
        );

        assertEquals(1, result.evaluatedQueries());
        assertEquals(1.0, result.precisionAtK(), 0.0001);
    }

    @Test
    void shouldRejectInvalidK() {

        InvertedIndex index = new InvertedIndex();

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        index,
                        List.of(),
                        Map.of(),
                        0
                )
        );
    }

    @Test
    void shouldRejectEvaluationWithoutJudgedQueries() {

        InvertedIndex index = new InvertedIndex();

        assertThrows(
                IllegalStateException.class,
                () -> evaluator.evaluate(
                        index,
                        List.of(new SciDocsQuery("q1", "java")),
                        Map.of(),
                        10
                )
        );
    }
}
