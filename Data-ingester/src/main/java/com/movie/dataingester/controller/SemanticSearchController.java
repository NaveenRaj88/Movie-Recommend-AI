package com.movie.dataingester.controller;

import com.movie.dataingester.dao.MovieJdbcDao;
import com.movie.dataingester.model.SemanticSearchRequest;
import com.movie.dataingester.model.SemanticSearchResponse;
import com.movie.dataingester.service.NomicTextEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SemanticSearchController {


    private final NomicTextEmbeddingService nomicTextEmbeddingService;

    private final MovieJdbcDao movieJdbcDao;

    @PostMapping("/semanticSearch")
    public ResponseEntity<List<SemanticSearchResponse>>  searchMovies(@RequestBody(required = true) SemanticSearchRequest semanticSearchRequest){
        log.info("Received the request for semantic search with semantic text {}", semanticSearchRequest.getSearchText());
        float[] searchVector = nomicTextEmbeddingService.createSearchVector(semanticSearchRequest.getSearchText());
        List<SemanticSearchResponse> semanticSearchResponses = movieJdbcDao.getSearchResponse(searchVector);
        return ResponseEntity.of(Optional.ofNullable(semanticSearchResponses));
    }
}
