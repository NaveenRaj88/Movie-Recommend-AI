package com.movie.dataingester.stream;

import com.movie.dataingester.dao.MovieJdbcDao;
import com.movie.dataingester.model.Movie;
import com.movie.dataingester.model.MovieEmbeddingEvent;
import com.movie.dataingester.model.MovieEmbeddings;
import com.movie.dataingester.model.OutboxEvent;
import com.movie.dataingester.service.NomicTextEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieEmbeddingEventConsumer {

    private static final String MODEL_NAME = "nomic-embed-text";

    private final MovieJdbcDao movieJdbcDao;

    private final NomicTextEmbeddingService nomicTextEmbeddingService;


    @KafkaListener(topics = "movie-outbox-events", groupId = "outbox-event-group")
    public void consumeMovieEmbeddingEvents(List<MovieEmbeddingEvent> movieEmbeddingEvents, Acknowledgment ack) {
        log.info("received outbox events of size {}", movieEmbeddingEvents.size());
        List<Long> movieIds = movieEmbeddingEvents.stream().map(MovieEmbeddingEvent::movieId).toList();
        Map<Long, Movie> movies = movieJdbcDao.getMovieRecords(movieIds);
        for(MovieEmbeddingEvent mee: movieEmbeddingEvents){
            if(!movies.containsKey(mee.movieId())){
                log.error("Severe error, Movie with id {} not found in the Movies table", mee.movieId());
                continue;
            }
            Movie movie = movies.get(mee.movieId());
            MovieEmbeddings movieEmbeddings = MovieEmbeddings.builder()
                    .movieId(movie.id())
                    .embedding(getEmbeddingVector(movie))
                    .modelName(MODEL_NAME)
                    .contentHash(Movie.computeEmbeddingHash(movie))
                    .status("COMPLETED")
                    .attemptCount(entries.getVaglue().attempts()+1)
                    .lastError(entries.getValue().lastError())
                    .build();

        }).toList();
        int inserted = movieJdbcDao.saveMovieEmbeddings(movieEmbeddingsList);
        log.info("inserted {} records ot the movie embeddings", inserted);
    }


    private float[] getEmbeddingVector(Movie m){
        return nomicTextEmbeddingService.createVector(getEmbeddingInputText(m));
    }
    private String getEmbeddingInputText(Movie m) {
        StringBuilder strb = new StringBuilder();
        strb.append("Title: ").append(m.original_title());
        strb.append("Language: ").append(m.original_language());
        strb.append("Genre: ").append(String.join(",", m.genre()));
        strb.append("Overview").append(m.overview());
        strb.append("Tagline: ").append(m.tagline());
        return strb.toString();
    }

}
