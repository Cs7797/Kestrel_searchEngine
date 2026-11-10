
package com.kestrel.controller;

import com.kestrel.config.KestrelConfig;
import com.kestrel.exception.ApiExceptionHandler;
import com.kestrel.repository.DocumentRepository;
import com.kestrel.service.KestrelService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({
        KestrelService.class,
        KestrelConfig.class,
        ApiExceptionHandler.class
})
class SearchControllerTest {

    @MockitoBean
    private DocumentRepository documentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIngestDocumentAndFindItThroughRankedSearch()
            throws Exception {

        when(documentRepository.existsById("doc-1"))
                .thenReturn(false);

        String requestBody = """
                {
                    "id": "doc-1",
                    "title": "Java and Spring",
                    "content": "Java is used to build backend applications.",
                    "url": "https://example.com/java"
                }
                """;

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Document created and indexed successfully"))
                .andExpect(jsonPath("$.id").value("doc-1"))
                .andExpect(jsonPath("$.title").value("Java and Spring"));

        mockMvc.perform(get("/api/search/ranked")
                        .param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("doc-1"));
    }

    @Test
    void shouldRejectDuplicateDocumentId()
            throws Exception {

        when(documentRepository.existsById("doc-1"))
                .thenReturn(true);

        String requestBody = """
                {
                    "id": "doc-1",
                    "title": "Java and Spring",
                    "content": "Java backend applications.",
                    "url": "https://example.com/java"
                }
                """;

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectBlankDocumentId() throws Exception {

        String requestBody = """
                {
                    "id": " ",
                    "title": "Java Guide",
                    "content": "Java backend",
                    "url": null
                }
                """;

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("id: Document ID must not be blank"));
    }

    @Test
    void shouldRejectMissingDocumentContent() throws Exception {

        String requestBody = """
                {
                    "id": "doc-2",
                    "title": "Java Guide",
                    "url": null
                }
                """;

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
