package com.movie.dataingester.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MovieEmbeddingEvent( Long eventId,
                                   Long movieId,
                                   String eventType,
                                   String embeddingContentHash,
                                   LocalDateTime createdAt) {
}
