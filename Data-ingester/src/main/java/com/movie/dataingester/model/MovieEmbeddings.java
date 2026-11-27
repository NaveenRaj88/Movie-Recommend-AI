package com.movie.dataingester.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MovieEmbeddings(
        long movieId,
        float[] embedding,
        String modelName,
        String contentHash,
        String status,
        Integer attemptCount,
        String lastError,
        LocalDateTime embeddedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
