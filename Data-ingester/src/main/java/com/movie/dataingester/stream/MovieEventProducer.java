package com.movie.dataingester.stream;

import com.movie.dataingester.model.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieEventProducer {

    public static final String TOPIC = "movie-ingestion-events";

    private final KafkaTemplate<String, Movie> kafkaTemplate;

    public void publishMovieEvents(Movie movie){
        kafkaTemplate.send(TOPIC, String.valueOf(movie.id()), movie)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish movie event for movie ID: {}", movie.id(), ex);
                    } else {
                        log.info("Successfully published movie event for movie ID: {}", movie.id());
                    }
                });
    }
}
