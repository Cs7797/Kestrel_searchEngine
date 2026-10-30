package com.kestrel.controller;

import com.kestrel.config.KestrelConfig;
import com.kestrel.service.KestrelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.kestrel.exception.ApiExceptionHandler;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.kestrel.repository.DocumentRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({KestrelService.class, KestrelConfig.class,  ApiExceptionHandler.class})
class SearchControllerTest {

     @MockitoBean
     private DocumentRepository documentRepository;
    
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIngestDocumentAndFindItThroughRankedSearch()
                    throws Exception {

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
                            .andExpect(content().string(""));

            mockMvc.perform(get("/api/search/ranked")
                            .param("q", "java"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$[0]").value("doc-1"));

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
    .andExpect(jsonPath("$.title").value("Validation failed"))
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