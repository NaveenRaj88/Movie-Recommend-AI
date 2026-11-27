package com.movie.dataingester.model;

public record OutboxEventPayload(long movieId, String embeddingContentHash) {

}
