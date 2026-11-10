
package com.kestrel;

import com.kestrel.dto.DocumentRequest;
import com.kestrel.entity.DocumentEntity;
import com.kestrel.repository.DocumentRepository;
import com.kestrel.service.KestrelService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/kestrel_test",
    "spring.flyway.url=jdbc:postgresql://127.0.0.1:5432/kestrel_test"
})
class KestrelIntegrationTest {

    @Autowired
    private KestrelService kestrelService;

    @Autowired
    private DocumentRepository documentRepository;

    private String documentId;

    @BeforeEach
    void setUp() {
        documentId = "integration-" + UUID.randomUUID();
    }

    @Test
    void shouldPersistDocumentInPostgreSQL() {

        DocumentRequest request = new DocumentRequest(
                documentId,
                "Integration Testing",
                "Testing Kestrel with PostgreSQL",
                "https://example.com/integration"
        );

        kestrelService.addDocument(request);

        DocumentEntity saved = documentRepository.findById(documentId)
                .orElseThrow();

        assertEquals("Integration Testing", saved.getTitle());
        assertEquals("Testing Kestrel with PostgreSQL", saved.getContent());
    }

    @Test
    void shouldRejectDuplicateDocumentId() {

        DocumentRequest request = new DocumentRequest(
                documentId,
                "Java Guide",
                "Java programming",
                null
        );

        kestrelService.addDocument(request);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> kestrelService.addDocument(request)
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void shouldUpdateDocumentInPostgreSQLAndIndex() {

        DocumentRequest original = new DocumentRequest(
                documentId,
                "Old Java Guide",
                "Java programming basics",
                null
        );

        kestrelService.addDocument(original);

        DocumentRequest updated = new DocumentRequest(
                documentId,
                "Python Guide",
                "Python programming basics",
                null
        );

        kestrelService.updateDocument(documentId, updated);

        DocumentEntity saved = documentRepository.findById(documentId)
                .orElseThrow();

        assertEquals("Python Guide", saved.getTitle());
        assertTrue(kestrelService.search("Python").contains(documentId));
        assertFalse(kestrelService.search("Java").contains(documentId));
    }

    @Test
    void shouldDeleteDocumentFromPostgreSQLAndIndex() {

        DocumentRequest request = new DocumentRequest(
                documentId,
                "Delete Test",
                "Temporary document",
                null);

        kestrelService.addDocument(request);

        kestrelService.deleteDocument(documentId);

        assertFalse(documentRepository.existsById(documentId));
        assertFalse(kestrelService.search("Temporary").contains(documentId));
    }
    @AfterEach
    void cleanUp() {
        if (documentRepository.existsById(documentId)) {
            kestrelService.deleteDocument(documentId);
        }
    }
@Test
void shouldReturn404WhenUpdatingNonexistentDocument() {

    DocumentRequest request = new DocumentRequest(
            documentId,
            "Updated Document",
            "Updated content",
            null);

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> kestrelService.updateDocument(documentId, request));

    assertEquals(404, exception.getStatusCode().value());
}
@Test
void shouldReturn404WhenDeletingNonexistentDocument() {

    ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> kestrelService.deleteDocument(documentId));

    assertEquals(404, exception.getStatusCode().value());
}
@Test
void shouldAllowDifferentIdsWithIdenticalContent() {

    String secondId = "integration-" + UUID.randomUUID();

    DocumentRequest first = new DocumentRequest(
            documentId,
            "Java Guide",
            "Java programming fundamentals",
            null
    );

    DocumentRequest second = new DocumentRequest(
            secondId,
            "Java Guide",
            "Java programming fundamentals",
            null
    );

    kestrelService.addDocument(first);
    kestrelService.addDocument(second);

    assertTrue(documentRepository.existsById(documentId));
    assertTrue(documentRepository.existsById(secondId));

    assertTrue(kestrelService.search("Java").contains(documentId));
    assertTrue(kestrelService.search("Java").contains(secondId));

    // Clean up the second document
    kestrelService.deleteDocument(secondId);
}
}
