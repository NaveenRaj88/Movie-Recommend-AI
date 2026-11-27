package com.movie.dataingester.model;

import lombok.Builder;

@Builder
public record OutboxEvent(
    Long id,
    String aggregateType,
    Long aggregateId,
    String eventType,
    String payload,
    OutboxStatusEnum status,
    Integer attempts,
    java.time.LocalDateTime createdAt,
    java.time.LocalDateTime publishedAt,
    String lastError
){

}
