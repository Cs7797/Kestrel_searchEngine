
package com.kestrel;

import com.kestrel.dto.DocumentRequest;
import com.kestrel.entity.DocumentEntity;
import com.kestrel.repository.DocumentRepository;
import com.kestrel.service.KestrelService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

class KestrelServiceTest {

    private InvertedIndex invertedIndex;
    private DocumentRepository documentRepository;
    private KestrelService kestrelService;

    @BeforeEach
    void setUp() {
        invertedIndex = new InvertedIndex();
        documentRepository = mock(DocumentRepository.class);

        kestrelService = new KestrelService(
                invertedIndex,
                documentRepository
        );  
    }
@Test
void shouldAddDocumentToIndexAndRepository() {

    DocumentRequest request = new DocumentRequest(
            "doc1",
            "Java Programming",
            "Java is a programming language",
            "https://example.com"
    );

    when(documentRepository.existsById("doc1")).thenReturn(false);

    kestrelService.addDocument(request);

    // Verify database persistence
    verify(documentRepository).saveAndFlush(any(DocumentEntity.class));

    // Verify document was added to the search index
    assertTrue(invertedIndex.search("Java").contains("doc1"));
}

@Test
void deleteRemovesDocumentCompletelyFromInMemoryIndex() {
    DocumentEntity entity = new DocumentEntity("delete-me", "unique title", "unique body", null);
    invertedIndex.addDocument(new Document("delete-me", "unique title", "unique body", null));
    invertedIndex.addDocument(new Document("keep", "other title", "shared body", null));
    when(documentRepository.findById("delete-me")).thenReturn(Optional.of(entity));

    kestrelService.deleteDocument("delete-me");

    assertTrue(invertedIndex.search("unique").isEmpty());
    assertEquals(0, invertedIndex.getTermFrequency("unique", "delete-me"));
    assertEquals(List.of("keep"), invertedIndex.search("shared"));
}

@Test
void repeatedUpdatesReplaceTheSameIndexEntry() {
    DocumentEntity original = new DocumentEntity("update-me", "old title", "old body", null);
    invertedIndex.addDocument(new Document("update-me", "old title", "old body", null));
    when(documentRepository.findById("update-me")).thenReturn(Optional.of(original));
    DocumentRequest firstReplacement = new DocumentRequest("update-me", "intermediate title", "intermediate body", null);
    DocumentRequest finalReplacement = new DocumentRequest("update-me", "final title phrase", "final body content", null);

    kestrelService.updateDocument("update-me", firstReplacement);
    kestrelService.updateDocument("update-me", finalReplacement);

    assertTrue(invertedIndex.search("old").isEmpty());
    assertTrue(invertedIndex.search("intermediate").isEmpty());
    assertEquals(List.of("update-me"), invertedIndex.search("final"));
    assertEquals(0, invertedIndex.getTermFrequency("old", "update-me"));
    assertEquals(0, invertedIndex.getTermFrequency("intermediate", "update-me"));
    assertEquals(List.of("update-me"), invertedIndex.searchPhrase("final title phrase"));
}

@Test
void repeatedRebuildReplacesPreviousIndexContentsWithDatabaseSnapshot() {
    String staleLongBody = "needle " + "archive ".repeat(100);
    invertedIndex.addDocument(new Document("stale", "stale needle title", staleLongBody, null));
    DocumentEntity first = new DocumentEntity("first", "first needle title", "needle first body", null);
    DocumentEntity shortDoc = new DocumentEntity("short", "needle short", "needle", null);
    DocumentEntity longDoc = new DocumentEntity("long", "needle long", "needle " + "filler ".repeat(50), null);
    when(documentRepository.findAll()).thenReturn(List.of(first)).thenReturn(List.of(shortDoc, longDoc));

    kestrelService.rebuildIndex();
    assertTrue(invertedIndex.search("stale").isEmpty());
    assertEquals(List.of("first"), invertedIndex.search("first"));
    InvertedIndex firstSnapshot = new InvertedIndex();
    firstSnapshot.addDocument(new Document("first", "first needle title", "needle first body", null));
    assertEquals(firstSnapshot.searchRanked("needle"), invertedIndex.searchRanked("needle"));

    kestrelService.rebuildIndex();
    assertTrue(invertedIndex.search("first").isEmpty());
    assertEquals(List.of("short"), invertedIndex.search("short"));
    assertEquals(List.of("long"), invertedIndex.search("long"));
    InvertedIndex currentSnapshot = new InvertedIndex();
    currentSnapshot.addDocument(new Document("short", "needle short", "needle", null));
    currentSnapshot.addDocument(new Document("long", "needle long", "needle " + "filler ".repeat(50), null));
    assertEquals(currentSnapshot.searchRanked("needle"), invertedIndex.searchRanked("needle"),
            "ranking after rebuild should match a fresh index's document count and field lengths");
    assertEquals(List.of("short"), invertedIndex.searchPhrase("needle short"));
}
}
