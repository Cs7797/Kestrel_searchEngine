package com.kestrel.controller;

import com.kestrel.config.FrontendCorsConfig;
import com.kestrel.dto.SearchResult;
import com.kestrel.service.KestrelService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@Import(FrontendCorsConfig.class)
class FrontendIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean KestrelService service;

    @Test
    void exposesDetailedSearchToConfiguredLocalFrontend() throws Exception {
        when(service.searchDetailed("java concurrency")).thenReturn(List.of(
                new SearchResult("doc-1", "Java concurrency", "A preview", null, 1)));
        mvc.perform(get("/api/search/results").param("q", "java concurrency")
                .header("Origin", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(jsonPath("$[0].id").value("doc-1"))
                .andExpect(jsonPath("$[0].title").value("Java concurrency"))
                .andExpect(jsonPath("$[0].snippet").value("A preview"))
                .andExpect(jsonPath("$[0].rank").value(1));
    }

    @Test
    void allowsDeployedVercelFrontend() throws Exception {
        when(service.searchDetailed("java")).thenReturn(List.of());
        mvc.perform(get("/api/search/results").param("q", "java")
                .header("Origin", "https://kestrel-search-engine.vercel.app"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://kestrel-search-engine.vercel.app"));
    }

    @Test
    void allowsReadPreflightFromVercel() throws Exception {
        mvc.perform(options("/api/search/results")
                .header("Origin", "https://kestrel-search-engine.vercel.app")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://kestrel-search-engine.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void rejectsWritePreflightFromVercel() throws Exception {
        mvc.perform(options("/api/documents")
                .header("Origin", "https://kestrel-search-engine.vercel.app")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnconfiguredOrigins() throws Exception {
        mvc.perform(get("/api/search/results").param("q", "java")
                .header("Origin", "https://unconfigured.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void rejectsCrossOriginWritePreflight() throws Exception {
        mvc.perform(options("/api/documents")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
