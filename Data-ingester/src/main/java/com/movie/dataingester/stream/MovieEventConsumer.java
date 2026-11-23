package com.movie.dataingester.stream;

import com.movie.dataingester.dao.MovieJdbcDao;
import com.movie.dataingester.model.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieEventConsumer {

    private final MovieJdbcDao movieJdbcDao;

    @KafkaListener(topics = MovieEventProducer.TOPIC, groupId = "movie-ingestion-group")
    public void consumeMovieBatch(List<Movie> movies, Acknowledgment ack) {
        log.info("Received kafka batch of {} movie events", movies.size());
        Map<Long, Movie> dedupicatedMovies = movies.stream().collect(Collectors.toMap(Movie::id, movie -> movie,
                (existing, replacement) -> replacement
        ));
        int inserted = movieJdbcDao.saveAllBatches(List.of(dedupicatedMovies));
        log.info("Inserted {} movies into the database.", inserted);
        ack.acknowledge();

        log.info("Committed Kafka batch: polled={}, deduplicated={}, inserted={}",
                movies.size(), dedupicatedMovies.size(), inserted);
    }
}
