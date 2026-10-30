package com.kestrel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentRequest(
        @NotBlank(message = "Document ID must not be blank")
        String id,

        @NotBlank(message = "Title must not be blank")
        String title,

        @NotNull(message = "Content must not be null")
        String content,

        String url
) {
}