package com.kestrel.controller;

import com.kestrel.service.KestrelService;
import com.kestrel.dto.DocumentRequest;
import com.kestrel.dto.DocumentResponse;
import com.kestrel.entity.DocumentEntity;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController 
public class SearchController {

    private final KestrelService kestrelService;

    public SearchController(KestrelService kestrelService) {
        this.kestrelService = kestrelService;
    }

    @GetMapping ("/api/search/ranked")
    public List<String> searchRanked(
            @RequestParam("q") String query) {
        return kestrelService.search(query);
    }

    
    @PostMapping("/api/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<DocumentResponse> addDocument(@Valid @RequestBody DocumentRequest request) {
       DocumentResponse response = kestrelService.addDocument(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

@PutMapping("/api/documents/{id}")
public ResponseEntity<DocumentResponse> updateDocument(
        @PathVariable String id,
        @Valid @RequestBody DocumentRequest request) {

    DocumentResponse response = kestrelService.updateDocument(id, request);

    return ResponseEntity.ok(response);
}

@DeleteMapping("/api/documents/{id}")
public ResponseEntity<DocumentResponse> deleteDocument(
        @PathVariable String id) {

    DocumentResponse response = kestrelService.deleteDocument(id);

    return ResponseEntity.ok(response);
}
@GetMapping("/api/documents")
public List<DocumentEntity> getAllDocuments() {
    return kestrelService.getAllDocuments();
}

}
