package com.movie.dataingester.stream;

import com.movie.dataingester.dao.MovieJdbcDao;
import com.movie.dataingester.model.Movie;
import com.movie.dataingester.model.MovieEmbeddingEvent;
import com.movie.dataingester.model.MovieEmbeddings;
import com.movie.dataingester.service.NomicTextEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieEmbeddingEventConsumer {

    private static final String MODEL_NAME = "nomic-embed-text";

    private final MovieJdbcDao movieJdbcDao;

    private final NomicTextEmbeddingService nomicTextEmbeddingService;


    @KafkaListener(topics = "movie-embedding-events", groupId = "outbox-event-group", properties = {"max.poll.records=100"})
    public void consumeMovieEmbeddingEvents(List<ConsumerRecord<String, MovieEmbeddingEvent>> records, Acknowledgment ack) {
        log.info("received outbox events of size {}", records.size());

        List<Long> movieIds = records.stream().map(r -> r.value().movieId()).toList();
        Map<Long, Movie> movies = movieJdbcDao.getMovieRecords(movieIds);
        List<MovieEmbeddings> movieEmbeddingsList = new ArrayList<>();
        for (ConsumerRecord<String, MovieEmbeddingEvent> record : records) {
            log.info(
                    "Received Kafka event | topic={} | partition={} | offset={} | key={}",
                    record.topic(), record.partition(), record.offset(), record.key()
            );
            if (!movies.containsKey(record.value().movieId())) {
                log.error("Severe error, Movie with id {} not found in the Movies table", record.value().movieId());
                continue;
            }
            Movie movie = movies.get(record.value().movieId());
            MovieEmbeddings movieEmbeddings = MovieEmbeddings.builder()
                    .movieId(movie.id())
                    .embedding(getEmbeddingVector(movie))
                    .modelName(MODEL_NAME)
                    .contentHash(Movie.computeEmbeddingHash(movie))
                    .status("COMPLETED")
                    .embeddedAt(LocalDateTime.now())
                    .build();
            movieEmbeddingsList.add(movieEmbeddings);
        }
        int inserted = movieJdbcDao.saveMovieEmbeddings(movieEmbeddingsList);
        log.info("inserted {} records ot the movie embeddings", inserted);
        ack.acknowledge();
    }


    private float[] getEmbeddingVector(Movie m) {
        return nomicTextEmbeddingService.createVector(getEmbeddingInputText(m));
    }

    private String getEmbeddingInputText(Movie m) {
        StringBuilder strb = new StringBuilder();
        strb.append("Title: ").append(m.original_title());
        strb.append("Language: ").append(m.original_language());
        if (m.genre() != null) {
            String genreStr = String.join(",", m.genre());
            strb.append("Genre: ").append(genreStr);
        }
        strb.append("Overview").append(m.overview());
        strb.append("Tagline: ").append(m.tagline());
        return strb.toString();
    }


}
