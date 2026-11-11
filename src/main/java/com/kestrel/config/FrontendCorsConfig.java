package com.kestrel.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Read-only browser access. Add the exact deployed frontend origin via environment. */
@Configuration
public class FrontendCorsConfig implements WebMvcConfigurer {
    private final String[] origins;

    public FrontendCorsConfig(@Value("${kestrel.frontend-origins:http://localhost:5173,http://127.0.0.1:5173,https://kestrel-search-engine.vercel.app}") String origins) {
        this.origins = Arrays.stream(origins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
        if (Arrays.stream(this.origins).anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("Frontend CORS origins must be exact origins, without wildcards");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET").allowCredentials(false);
        registry.addMapping("/actuator/health").allowedOrigins(origins).allowedMethods("GET").allowCredentials(false);
    }
}
