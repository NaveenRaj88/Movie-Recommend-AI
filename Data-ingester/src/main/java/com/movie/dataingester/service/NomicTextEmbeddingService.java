package com.movie.dataingester.service;


import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NomicTextEmbeddingService {

    private static final String SEARCH_QUERY = "search_query: ";
    private static final String SEARCH_DOCUMENT = "search_document: ";
    private final OllamaEmbeddingModel ollamaEmbeddingModel;

    public NomicTextEmbeddingService(OllamaEmbeddingModel ollamaEmbeddingModel){
        this.ollamaEmbeddingModel = ollamaEmbeddingModel;
    }

    public float[] createIndexingVector(String text){
        Embedding embedding = ollamaEmbeddingModel.embed(SEARCH_DOCUMENT + text).content();
        return embedding.vector();
    }

    public float[] createSearchVector(String text){
        Embedding embedding = ollamaEmbeddingModel.embed(SEARCH_QUERY + text).content();
        return embedding.vector();
    }
}
