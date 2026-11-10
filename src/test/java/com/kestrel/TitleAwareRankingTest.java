
package com.kestrel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TitleAwareRankingTest {

    @Test
    void zeroTitleBoostUsesOnlyBodyMatches() {
        InvertedIndex index = new InvertedIndex(0.0);
        index.addDocument(new Document("title-only", "rare concept", "unrelated", null));
        index.addDocument(new Document("body-match", "unrelated", "rare concept", null));

        assertEquals(List.of("body-match"), index.searchRanked("rare concept"));
    }

    @Test
    void titleBoostMustBeFiniteAndNonNegative() {
        assertThrows(IllegalArgumentException.class, () -> new InvertedIndex(-1.0));
        assertThrows(IllegalArgumentException.class, () -> new InvertedIndex(Double.NaN));
    }

    @Test
    void documentWithQueryTermInTitleShouldRankHigher() {
        InvertedIndex index = new InvertedIndex();

        Document titleMatch = new Document(
                "D1",
                "distributed systems",
                "alpha beta",
                null
        );

        Document bodyMatch = new Document(
                "D2",
                "alpha beta",
                "distributed systems",
                null
        );

        index.addDocument(titleMatch);
        index.addDocument(bodyMatch);

        List<String> results = index.searchRanked("distributed systems");

        assertEquals("D1", results.get(0));
    }

    @Test
    void searchShouldFindTermsInBothFields() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1", "distributed systems", "alpha beta", null
        ));

        index.addDocument(new Document(
                "D2", "alpha beta", "distributed systems", null
        ));

        assertEquals(
                List.of("D1", "D2"),
                index.search("distributed")
        );
    }

    @Test
    void phraseSearchShouldWorkInTitle() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1",
                "distributed systems research",
                "unrelated content",
                null
        ));

        assertEquals(
                List.of("D1"),
                index.searchPhrase("distributed systems")
        );
    }

    @Test
    void phraseSearchShouldWorkInBody() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1",
                "unrelated title",
                "research in distributed systems",
                null
        ));

        assertEquals(
                List.of("D1"),
                index.searchPhrase("distributed systems")
        );
    }

    @Test
    void updatingDocumentShouldRemoveOldTitleAndBodyPostings() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1",
                "oldtitle",
                "oldcontent",
                null
        ));

        index.addDocument(new Document(
                "D1",
                "newtitle",
                "newcontent",
                null
        ));

        assertFalse(index.search("oldtitle").contains("D1"));
        assertFalse(index.search("oldcontent").contains("D1"));

        assertTrue(index.search("newtitle").contains("D1"));
        assertTrue(index.search("newcontent").contains("D1"));
    }

    @Test
    void deletingDocumentShouldRemoveBothFieldPostings() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1",
                "distributed systems",
                "machine learning",
                null
        ));

        index.removeDocument("D1");

        assertTrue(index.search("distributed").isEmpty());
        assertTrue(index.search("machine").isEmpty());
        assertTrue(index.searchRanked("distributed systems").isEmpty());
    }

    @Test
    void termFrequencyShouldIncludeTitleAndBody() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(new Document(
                "D1",
                "machine learning",
                "machine learning algorithms",
                null
        ));

        assertEquals(2, index.getTermFrequency("machine", "D1"));
        assertEquals(2, index.getTermFrequency("learning", "D1"));
    }
}
