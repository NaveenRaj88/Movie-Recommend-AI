package com.movie.dataingester.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class SemanticSearchRequest {

    private String searchText;
}
