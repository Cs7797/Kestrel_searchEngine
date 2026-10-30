package com.kestrel.config;

import com.kestrel.service.KestrelService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexInitializer {

    @Bean
    public ApplicationRunner initializeIndex(KestrelService kestrelService) {
        return args -> {
            kestrelService.rebuildIndex();
            System.out.println("Kestrel search index initialized successfully.");
        };
    }
}