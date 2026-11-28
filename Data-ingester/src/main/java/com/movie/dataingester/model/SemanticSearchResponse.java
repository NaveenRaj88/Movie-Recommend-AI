package com.movie.dataingester.model;

import lombok.Builder;

@Builder
public record SemanticSearchResponse(
        Long id,
        String original_title,
        String original_language,
        String[] genres,
        String overview,
        Double cosineDistance

) {
}
