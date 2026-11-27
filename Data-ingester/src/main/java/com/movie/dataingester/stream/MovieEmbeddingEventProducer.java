package com.movie.dataingester.stream;


import com.movie.dataingester.dao.MovieJdbcDao;
import com.movie.dataingester.model.MovieEmbeddingEvent;
import com.movie.dataingester.model.OutboxEvent;
import com.movie.dataingester.model.OutboxEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieEmbeddingEventProducer {

    private static final String OUTBOX_KAFKA_TOPIC = "movie-embedding-events";

    private final MovieJdbcDao movieJdbcDao;

    private final KafkaTemplate<String, MovieEmbeddingEvent> kafkaTemplate;


    private final  ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void publishOutboxEvents() {
        Map<Long, OutboxEvent> outboxEventMap = movieJdbcDao.getMovieOutboxEventsBatch();
        for (Map.Entry<Long, OutboxEvent> outboxEntries : outboxEventMap.entrySet()) {
            MovieEmbeddingEvent movieEmbeddingEvent = MovieEmbeddingEvent.builder()
                    .eventId(outboxEntries.getValue().id())
                    .movieId(outboxEntries.getValue().aggregateId())
                    .eventType("Movie-Embedding-Request")
                    .embeddingContentHash(objectMapper.readValue(outboxEntries.getValue().payload(), OutboxEventPayload.class).embeddingContentHash())
                    .createdAt(LocalDateTime.now())
                    .build();
            kafkaTemplate.send(OUTBOX_KAFKA_TOPIC, String.valueOf(movieEmbeddingEvent.movieId()), movieEmbeddingEvent);
            movieJdbcDao.updateOutboxEventStatus(outboxEntries.getKey());
        }

    }


}
