
package com.kestrel;

import com.kestrel.dto.DocumentRequest;
import com.kestrel.repository.DocumentRepository;
import com.kestrel.service.KestrelService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        kestrelService.addDocument(request);

        // Verify database persistence was requested
        verify(documentRepository, times(1))
                .save(any());

        // Verify document was added to the search index
        assertTrue(invertedIndex.search("Java").contains("doc1"));
    }
}
