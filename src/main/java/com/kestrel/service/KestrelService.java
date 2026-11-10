
package com.kestrel.service;

import com.kestrel.Document;
import com.kestrel.InvertedIndex;
import com.kestrel.dto.DocumentRequest;
import com.kestrel.dto.DocumentResponse;
import com.kestrel.entity.DocumentEntity;
import com.kestrel.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class KestrelService {

    private final InvertedIndex invertedIndex;
    private final DocumentRepository documentRepository;

    public KestrelService(
            InvertedIndex invertedIndex,
            DocumentRepository documentRepository) {

        this.invertedIndex = invertedIndex;
        this.documentRepository = documentRepository;
    }

    // SEARCH
    public List<String> search(String query) {

        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search query cannot be empty"
            );
        }

        return invertedIndex.searchRanked(query);
    }

    // CREATE
    @Transactional
    public DocumentResponse addDocument(DocumentRequest request) {

        // Basic request validation
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document request cannot be empty"
            );
        }

        // Prevent duplicate document IDs
        if (documentRepository.existsById(request.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Document with ID " + request.id() + " already exists"
            );
        }

        Document document = new Document(
                request.id(),
                request.title(),
                request.content(),
                request.url()
        );

        DocumentEntity entity = new DocumentEntity(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getUrl()
        );

        // Flush surfaces database constraint violations promptly
        documentRepository.saveAndFlush(entity);

        // Update index only after successful transaction commit
        afterCommit(() -> invertedIndex.addDocument(document));

        return new DocumentResponse(
                "Document created and indexed successfully",
                document.getId(),
                document.getTitle()
        );
    }

    // READ ALL
    @Transactional(readOnly = true)
    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAll();
    }

    // READ ONE
    @Transactional(readOnly = true)
    public DocumentEntity getDocumentById(String id) {

        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document with ID " + id + " not found"
                ));
    }

    // UPDATE
    @Transactional
    public DocumentResponse updateDocument(
            String id,
            DocumentRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document request cannot be empty"
            );
        }

        // The document must already exist
        DocumentEntity existingEntity = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document with ID " + id + " not found"
                ));

        // Prevent accidental ID changes through the request body
        if (!id.equals(request.id())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document ID in request body must match the URL ID"
            );
        }

        Document newDocument = new Document(
                id,
                request.title(),
                request.content(),
                request.url()
        );

        DocumentEntity updatedEntity = new DocumentEntity(
                id,
                request.title(),
                request.content(),
                request.url()
        );

        documentRepository.saveAndFlush(updatedEntity);

        // Replace the old index entry after successful commit
        afterCommit(() -> invertedIndex.addDocument(newDocument));

        return new DocumentResponse(
                "Document updated successfully",
                id,
                updatedEntity.getTitle()
        );
    }

    // DELETE
    @Transactional
    public DocumentResponse deleteDocument(String id) {

        DocumentEntity entity = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document with ID " + id + " not found"
                ));

        Document document = toDocument(entity);

        documentRepository.delete(entity);

        // Remove from index only after successful commit
        afterCommit(() ->
                invertedIndex.removeDocumentFromIndex(document)
        );

        return new DocumentResponse(
                "Document deleted successfully",
                id,
                entity.getTitle()
        );
    }

    // REBUILD INDEX FROM DATABASE
    @Transactional(readOnly = true)
    public void rebuildIndex() {

        List<Document> snapshot = documentRepository.findAll().stream()
                .map(this::toDocument)
                .toList();

       afterCommit(() -> invertedIndex.replaceAllDocuments(snapshot));
    }

    // CONVERT JPA ENTITY TO SEARCH DOMAIN MODEL
    private Document toDocument(DocumentEntity entity) {

        return new Document(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getUrl()
        );
    }

    // RUN INDEX OPERATIONS AFTER DATABASE COMMIT
    private void afterCommit(Runnable action) {

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );

        } else {
            action.run();
        }
    }
}
