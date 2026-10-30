
package com.kestrel.service;

import com.kestrel.Document;
import com.kestrel.InvertedIndex;
import com.kestrel.dto.DocumentRequest;
import com.kestrel.entity.DocumentEntity;
import com.kestrel.repository.DocumentRepository;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<String> search(String query) {
        return invertedIndex.searchRanked(query);
    }

    @Transactional
    public void addDocument(DocumentRequest request) {

        Document document = new Document(
                request.id(),
                request.title(),
                request.content(),
                request.url());

        DocumentEntity entity = new DocumentEntity(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getUrl());

        // Persist document in PostgreSQL
        documentRepository.save(entity);

        // Update in-memory search index
        invertedIndex.addDocument(document);
    }

    public void rebuildIndex() {
        List<DocumentEntity> documents = documentRepository.findAll();

        for (DocumentEntity entity : documents) {
            Document document = new Document(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getContent(),
                    entity.getUrl());

            invertedIndex.addDocument(document);
        }
    }

    @Transactional
    public void updateDocument(String id, DocumentRequest request) {

        if (!documentRepository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Document not found");
        }

        Document document = new Document(
                id,
                request.title(),
                request.content(),
                request.url());

        DocumentEntity entity = new DocumentEntity(
                id,
                request.title(),
                request.content(),
                request.url());

        documentRepository.save(entity);

        // addDocument handles replacement of an existing indexed document
        invertedIndex.addDocument(document);
    }

    @Transactional
    public void deleteDocument(String id) {

        DocumentEntity entity = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        Document document = new Document(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getUrl());

        documentRepository.deleteById(id);
        invertedIndex.removeDocumentFromIndex(document);
    }
}
