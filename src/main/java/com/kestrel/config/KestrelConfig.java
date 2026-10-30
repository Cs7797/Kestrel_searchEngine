package com.kestrel.config;

import com.kestrel.InvertedIndex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KestrelConfig {

    @Bean
    public InvertedIndex invertedIndex() {
        return new InvertedIndex();
    }
}