
package com.kestrel.controller;

import com.kestrel.service.KestrelService;
import com.kestrel.dto.DocumentRequest;
import com.kestrel.dto.DocumentResponse;
import com.kestrel.entity.DocumentEntity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.List;
import com.kestrel.dto.SearchResult;

@RestController
public class SearchController {

    private final KestrelService kestrelService;
    private final boolean writesEnabled;

    public SearchController(
            KestrelService kestrelService,
            @Value("${kestrel.writes-enabled:true}") boolean writesEnabled) {

        this.kestrelService = kestrelService;
        this.writesEnabled = writesEnabled;
    }

    @GetMapping("/")
public String home() {
    return "Kestrel Search Engine API is running";
}
    // Prevent unauthorized modifications in read-only deployments
    private void requireWritesEnabled() {

        if (!writesEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Document modifications are disabled"
            );
        }
    }

    // SEARCH DOCUMENTS
    @GetMapping("/api/search/ranked")
    public List<String> searchRanked(
            @RequestParam("q") String query) {

        return kestrelService.search(query);
    }

    // CREATE DOCUMENT
    @PostMapping("/api/documents")
    public ResponseEntity<DocumentResponse> addDocument(
            @Valid @RequestBody DocumentRequest request) {

        requireWritesEnabled();

        DocumentResponse response =
                kestrelService.addDocument(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // UPDATE DOCUMENT
    @PutMapping("/api/documents/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable String id,
            @Valid @RequestBody DocumentRequest request) {

        requireWritesEnabled();

        DocumentResponse response =
                kestrelService.updateDocument(id, request);

        return ResponseEntity.ok(response);
    }

    // DELETE DOCUMENT
    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<DocumentResponse> deleteDocument(
            @PathVariable String id) {

        requireWritesEnabled();

        DocumentResponse response =
                kestrelService.deleteDocument(id);

        return ResponseEntity.ok(response);
    }

    // GET ALL DOCUMENTS
    @GetMapping("/api/documents")
    public List<DocumentEntity> getAllDocuments() {

        return kestrelService.getAllDocuments();
    }

    // GET DOCUMENT BY ID
    @GetMapping("/api/documents/{id}")
    public ResponseEntity<DocumentEntity> getDocumentById(
            @PathVariable String id) {

        DocumentEntity document = kestrelService.getDocumentById(id);

        return ResponseEntity.ok(document);
    }
    
    @GetMapping("/api/search/results")
public List<SearchResult> searchResults(
        @RequestParam("q") String query) {

    return kestrelService.searchDetailed(query);
}
}
