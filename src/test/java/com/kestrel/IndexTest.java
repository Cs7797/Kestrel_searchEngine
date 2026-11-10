package com.kestrel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;


import org.junit.jupiter.api.Test;

public class IndexTest {
        // @Test
        // void checkIndex() {
        //         InvertedIndex index = new InvertedIndex();
        //         Document doc1 = new Document(
        //                         "D1", "Java Guide", "Java search Java", null);

        //         Document doc2 = new Document(
        //                         "D2", "Java Concurrency", "Java concurrency", null);
        //         index.addDocument(doc1);
        //         index.addDocument(doc2);
        //         List<String> Results = index.search("java");
        //         assertEquals(2, Results.size());
        //         assertTrue(Results.contains("D1"));
        //         assertTrue(Results.contains("D2"));
        // }

        

        // @Test
        // void searchForUnknownTermShouldReturnEmptyList() {
        //         InvertedIndex index = new InvertedIndex();
        //         index.addDocument(
        //                         new Document("D1", "Java Guide", "Java concurrency", null));

        //         assertTrue(index.search("python").isEmpty());
        // }

        // @Test
        // void searchForBlankTermShouldReturnEmptyList() {
        //         InvertedIndex index = new InvertedIndex();

        //         assertTrue(index.search("").isEmpty());
        //         assertTrue(index.search("   ").isEmpty());
        // }

        // @Test
        // void updatingDocumentShouldRemoveOldTerms() {
        //         InvertedIndex index = new InvertedIndex();

        //         index.addDocument(
        //                         new Document("D1", "Java Guide", "Java search", null));

        //         index.addDocument(
        //                         new Document("D1", "Python Guide", "Python search", null));

        //         assertTrue(index.search("java").isEmpty());
        //         assertTrue(index.search("python").contains("D1"));
        //         assertTrue(index.search("search").contains("D1"));
        // }

        // @Test
        // void updatingDocumentShouldPreserveTermsUsedByOtherDocuments() {
        //         InvertedIndex index = new InvertedIndex();

        //         index.addDocument(
        //                         new Document("D1", "Java Guide", "Java search", null));
        //         index.addDocument(
        //                         new Document("D2", "Java Concurrency", "Java concurrency", null));

        //         index.addDocument(
        //                         new Document("D1", "Python Guide", "Python search", null));

        //         assertTrue(index.search("java").contains("D2"));
        //         assertTrue(!index.search("java").contains("D1"));
        // }

        // @Test
        // void removingDocumentShouldRemoveItsTermsFromSearch() {
        //         InvertedIndex index = new InvertedIndex();

        //         index.addDocument(
        //                         new Document("D1", "Java Guide", "Java search", null));

        //         index.removeDocument("D1");

        //         assertTrue(index.search("java").isEmpty());
        //         assertTrue(index.search("search").isEmpty());
        // }

        // @Test
        // void removingDocumentShouldPreserveTermsUsedByOtherDocuments() {
        //         InvertedIndex index = new InvertedIndex();

        //         index.addDocument(
        //                         new Document("D1", "Java Guide", "Java search", null));
        //         index.addDocument(
        //                         new Document("D2", "Java Concurrency", "Java concurrency", null));

        //         index.removeDocument("D1");

        //         assertEquals(List.of("D2"), index.search("java"));
        //         assertTrue(index.search("search").isEmpty());
        // }

//         @Test
//         void removingUnknownDocumentShouldNotBreakIndex() {
//                 InvertedIndex index = new InvertedIndex();

//                 index.addDocument(
//                                 new Document("D1", "Java Guide", "Java search", null));

//                 index.removeDocument("D99");

//                 assertTrue(index.search("java").contains("D1"));
//         }

//         @Test
//         void shouldTrackTermFrequency() {
//                 InvertedIndex index = new InvertedIndex();
//                 Document doc = new Document(
//                                 "D1", "Java Guide", "Java search Java", null);

//                 index.addDocument(doc);

//                 assertEquals(2, index.getTermFrequency("java", "D1"));
//                 assertEquals(1, index.getTermFrequency("search", "D1"));
//                 assertEquals(0, index.getTermFrequency("python", "D1"));
//         }

//         @Test
//         void updatingDocumentShouldReplaceOldTermFrequencies() {
//                 InvertedIndex index = new InvertedIndex();

//                 index.addDocument(
//                                 new Document("D1", "Java Guide", "Java Java search", null));
//                 index.addDocument(
//                                 new Document("D1", "Python Guide", "Python search", null));

//                 assertEquals(0, index.getTermFrequency("java", "D1"));
//                 assertEquals(1, index.getTermFrequency("python", "D1"));
//                 assertEquals(1, index.getTermFrequency("search", "D1"));
//         }

//         @Test
//         void shouldStoreCorrectTermPositions() {
//                 InvertedIndex index = new InvertedIndex();

//                 Document doc = new Document(
//                                 "D1",
//                                 "Java Guide",
//                                 "java search java engine",
//                                 null);

//                 index.addDocument(doc);

//                 assertEquals(2, index.getTermFrequency("java", "D1"));
//                 assertEquals(1, index.getTermFrequency("search", "D1"));
//                 assertEquals(1, index.getTermFrequency("engine", "D1"));
//         }
//         @Test
// void shouldFindExactPhrase() {
//     InvertedIndex index = new InvertedIndex();

//     index.addDocument(
//             new Document("D1", "Guide", "java search engine", null)
//     );
//     index.addDocument(
//             new Document("D2", "Guide", "search java engine", null)
//     );
//     index.addDocument(
//             new Document("D3", "Guide", "java powerful search", null)
//     );

//     assertTrue(index.searchPhrase("java search").contains("D1"));
//     assertFalse(index.searchPhrase("java search").contains("D2"));
//     assertFalse(index.searchPhrase("java search").contains("D3"));
// }



// @Test
// void phraseSearchShouldReturnEmptyForBlankPhrase() {
//         InvertedIndex index = new InvertedIndex();

//         assertTrue(index.searchPhrase("").isEmpty());
//         assertTrue(index.searchPhrase("   ").isEmpty());
// }
@Test
void rankedSearchShouldReturnMatchingDocumentsByScore() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(
                        new Document("D1", "Java Guide",
                                        "java tutorial", null));

        index.addDocument(
                        new Document("D2", "Java Reference",
                                        "java java java tutorial", null));

        index.addDocument(
                        new Document("D3", "Python Guide",
                                        "python tutorial", null));

        List<String> results = index.searchRanked("java");

        assertEquals(2, results.size());
        assertTrue(results.contains("D1"));
        assertTrue(results.contains("D2"));
        assertFalse(results.contains("D3"));
}
@Test
void rankedSearchShouldReturnDocumentsInScoreOrder() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(
                        new Document("D1", "Java Guide", "java", null));
        index.addDocument(
                        new Document("D2", "Java Tutorial", "java java java", null));
        index.addDocument(
                        new Document("D3", "Python Guide", "python", null));

        List<String> results = index.searchRanked("java");

        assertEquals(2, results.size());
        assertEquals("D2", results.get(0));
        assertEquals("D1", results.get(1));
}
@Test
void rankedSearchShouldReflectDocumentUpdates() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(
                        new Document("D1", "Java Guide", "java java java", null));

        index.addDocument(
                        new Document("D2", "Java Tutorial", "java", null));

        // Replace D1 with new content using the same document ID
        index.addDocument(
                        new Document("D1", "Python Guide", "python", null));

        List<String> results = index.searchRanked("java");

        assertEquals(List.of("D2"), results);
}
@Test
void rankedSearchShouldReturnEmptyListWhenNoDocumentsMatch() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(
                        new Document("D1", "Java Guide", "java programming", null));

        List<String> results = index.searchRanked("python");

        assertEquals(List.of(), results);
}
@Test
void rankedSearchShouldCombineScoresForMultipleQueryTerms() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(
                        new Document("D1", "Java Guide", "java", null));

        index.addDocument(
                        new Document("D2", "Java Python Guide", "java python", null));

        List<String> results = index.searchRanked("java python");

        assertEquals(2, results.size());
        assertEquals("D2", results.get(0));
        assertEquals("D1", results.get(1));
}
@Test
void searchShouldBeCaseInsensitive() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
        new Document("D1", "Java Guide", "JAVA Java java", null)
    );

    assertEquals(List.of("D1"), index.search("java"));
    assertEquals(List.of("D1"), index.search("JAVA"));
}

@Test
void punctuationOnlyContentShouldNotCreateSearchableTerms() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
        new Document("D1", "Symbols", "!!! ... @#$", null)
    );

   assertEquals(List.of("D1"), index.search("symbols"));
    assertEquals(List.of(), index.search("java"));
}

@Test
void searchShouldHandleUnicodeWords() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(
                        new Document("D1", "Unicode", "café 東京", null));

        assertEquals(List.of("D1"), index.search("café"));
        assertEquals(List.of("D1"), index.search("東京"));
}
@Test
void updatingDocumentShouldRemoveOldTermsAndIndexNewTerms() {
        InvertedIndex index = new InvertedIndex();

        index.addDocument(
                        new Document("D1", "Java Guide", "java spring", null));

        // Replace D1 with completely different content
        index.addDocument(
                        new Document("D1", "Python Guide", "python django", null));

        assertEquals(List.of(), index.search("java"));
        assertEquals(List.of(), index.search("spring"));

        assertEquals(List.of("D1"), index.search("python"));
        assertEquals(List.of("D1"), index.search("django"));
}
@Test
void removingDocumentShouldPreserveTermsUsedByOtherDocuments() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
            new Document("D1", "Java Guide", "java spring", null));
    index.addDocument(
            new Document("D2", "Spring Guide", "java spring boot", null));

    index.removeDocument("D1");

    assertEquals(List.of(), index.search("java").stream()
            .filter(id -> id.equals("D1"))
            .toList());

    assertEquals(List.of("D2"), index.search("java"));
    assertEquals(List.of("D2"), index.search("spring"));
    assertEquals(List.of("D2"), index.search("boot"));
}
@Test
void emptyContentShouldNotCreateSearchableTerms() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
        new Document("D1", "Empty Document", "", null)
    );

    assertEquals(List.of(), index.search("java"));
    assertEquals(0, index.getTermFrequency("java", "D1"));
}

@Test
void punctuationOnlyQueryShouldReturnNoResults() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
            new Document("D1", "Java Guide", "java programming", null));

    assertEquals(List.of(), index.search("!!!"));
    assertEquals(List.of(), index.searchPhrase("..."));
    assertEquals(List.of(), index.searchRanked("!!!"));
}
@Test
void termFrequencyShouldCountRepeatedOccurrences() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
        new Document("D1", "Java Guide", "java java python java", null)
    );

   assertEquals(4, index.getTermFrequency("java", "D1"));
    assertEquals(1, index.getTermFrequency("python", "D1"));
    assertEquals(0, index.getTermFrequency("missing", "D1"));
}

@Test
void repeatedTermsShouldHaveCorrectPositions() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
            new Document("D1", "Java Guide", "java java java", null));

    assertEquals(List.of("D1"), index.searchPhrase("java java"));
    assertEquals(List.of("D1"), index.searchPhrase("java java java"));
    assertEquals(List.of(), index.searchPhrase("java java java java"));
}
@Test
void removingNonexistentDocumentShouldNotAffectIndex() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
            new Document("D1", "Java Guide", "java spring boot", null));

    index.removeDocument("D999");

    assertEquals(List.of("D1"), index.search("java"));
    assertEquals(List.of("D1"), index.search("spring"));
    assertEquals(2, index.getTermFrequency("java", "D1"));
}
@Test
void searchShouldMatchSingleTermsOnly() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(
            new Document("D1", "Java Spring Guide", "java spring boot", null));

    assertEquals(List.of("D1"), index.search("java"));
    assertEquals(List.of("D1"), index.search("spring"));
    assertEquals(List.of(), index.search("java spring"));
}
@Test
void addingNullDocumentShouldThrowException() {
    InvertedIndex index = new InvertedIndex();

    assertThrows(
            NullPointerException.class,
            () -> index.addDocument(null));
}
@Test
void shouldMatchSingularQueryWithPluralDocumentTerm() {
    InvertedIndex index = new InvertedIndex();

    Document document = new Document(
            "D1",
            "Networks",
            "Networks are essential for communication",
            null
    );

    index.addDocument(document);

    assertTrue(index.searchRanked("network").contains("D1"));
}

@Test
void shouldMatchPluralQueryWithSingularDocumentTerm() {
    InvertedIndex index = new InvertedIndex();

    Document document = new Document(
            "D2",
            "Network",
            "A network connects multiple devices",
            null);

    index.addDocument(document);

    assertTrue(index.searchRanked("networks").contains("D2"));
}

@Test
void phraseSearchShouldPreserveStopwords() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(new Document(
            "D1", "Guide", "state of the art search", null));

    assertEquals(
            List.of("D1"),
            index.searchPhrase("state of the art")
    );
}

@Test
void phraseSearchShouldSupportStemming() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(new Document(
            "D1", "Guide", "networks are growing", null));

    assertEquals(
            List.of("D1"),
            index.searchPhrase("network are growing")
    );
}

@Test
void termFrequencyShouldSupportStemming() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(new Document(
            "D1", "Guide", "network networks network", null));

    assertEquals(3, index.getTermFrequency("networks", "D1"));
}

@Test
void deletingStemmedDocumentShouldRemoveAllPostings() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(new Document(
            "D1", "Guide", "temporary networks", null));

    index.removeDocument("D1");

    assertTrue(index.search("temporary").isEmpty());
    assertTrue(index.search("network").isEmpty());
}

@Test
void updatingStemmedDocumentShouldRemoveOldPostings() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(new Document(
            "D1", "Guide", "temporary networks", null));

    index.addDocument(new Document(
            "D1", "Guide", "database systems", null));

    assertTrue(index.search("temporary").isEmpty());
    assertTrue(index.search("networks").isEmpty());
    assertEquals(List.of("D1"), index.search("database"));
}

@Test
void rankedSearchShouldIgnoreStopwordsAndDuplicateTerms() {
    InvertedIndex index = new InvertedIndex();

    index.addDocument(new Document(
            "D1", "Guide", "java programming", null));

    assertEquals(
            index.searchRanked("java java"),
            index.searchRanked("the java and java")
    );
}

}