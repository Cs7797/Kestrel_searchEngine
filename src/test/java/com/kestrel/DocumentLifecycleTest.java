package com.kestrel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentLifecycleTest {

    @Test
    void removalClearsAllFieldsAndLengthsWhilePreservingOtherPostings() {
        InvertedIndex index = new InvertedIndex();
        Document removed = new Document("removed", "unique title phrase", "shared needle", null);
        Document kept = new Document("kept", "shared heading", "shared needle remains", null);
        Document alsoKept = new Document("also-kept", "shared title", "needle with a longer body", null);
        index.addDocument(removed);
        index.addDocument(kept);
        index.addDocument(alsoKept);

        index.removeDocumentFromIndex(removed);

        assertEquals(0, index.getTermFrequency("unique", "removed"));
        assertEquals(0, index.getTermFrequency("needle", "removed"));
        assertTrue(index.search("unique").isEmpty());
        assertEquals(List.of("also-kept", "kept"), index.search("needle"));
        assertEquals(List.of("kept"), index.searchPhrase("shared needle remains"));

        InvertedIndex freshRemainingCorpus = new InvertedIndex();
        freshRemainingCorpus.addDocument(kept);
        freshRemainingCorpus.addDocument(alsoKept);
        assertEquals(freshRemainingCorpus.searchRanked("shared needle"),
                index.searchRanked("shared needle"),
                "ranking should use the remaining corpus statistics after deletion");
    }

    @Test
    void replacementRemovesOldPostingsAndStoresNewFieldLengths() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument(new Document("same", "old title", "old body", null));
        index.addDocument(new Document("same", "new title extended", "new body content", null));

        assertTrue(index.search("old").isEmpty());
        assertEquals(List.of("same"), index.search("new"));
        assertEquals(0, index.getTermFrequency("old", "same"));
        assertEquals(2, index.getTermFrequency("new", "same"));
        assertEquals(List.of("same"), index.searchRanked("new body"));
        assertTrue(index.searchPhrase("old title").isEmpty());
        assertEquals(List.of("same"), index.searchPhrase("new title extended"));
    }

    @Test
    void phraseSearchRemainsAdjacentAndFieldLocal() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument(new Document("phrase", "red fox", "brown swiftly jumps", null));

        assertEquals(List.of("phrase"), index.searchPhrase("red fox"));
        assertTrue(index.searchPhrase("red jumps").isEmpty(), "phrases must not cross title/body boundaries");
        assertTrue(index.searchPhrase("brown jumps").isEmpty(), "all phrase terms must be adjacent");
        assertEquals(List.of("phrase"), index.searchPhrase("brown swiftly jumps"));
    }

    @Test
    void defaultRankingMatchesExplicitProductionSeparateFieldConfiguration() {
        InvertedIndex defaultIndex = new InvertedIndex();
        InvertedIndex explicitProduction = new InvertedIndex(2.0);
        List<Document> documents = List.of(
                new Document("a", "query alpha", "body one", null),
                new Document("b", "other title", "query alpha body two", null),
                new Document("c", "query beta", "body three", null));
        documents.forEach(defaultIndex::addDocument);
        documents.forEach(explicitProduction::addDocument);

        assertEquals(List.of("a", "b", "c"), defaultIndex.searchRanked("query alpha"));
        assertEquals(explicitProduction.searchRanked("query alpha"), defaultIndex.searchRanked("query alpha"));
    }
}
