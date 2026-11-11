
package com.kestrel.dto;

public record SearchResult(
        String id,
        String title,
        String snippet,
        String url,
        int rank
) {}
