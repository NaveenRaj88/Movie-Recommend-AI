package com.movie.dataingester.configuration;

import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Slf4j
@Configuration
public class DataIngesterConfiguration {

    @Bean
    public NewTopic movieIngestionTopic() {
        return TopicBuilder.name("movie-ingestion-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic movieIngestionDltTopic() {
        return TopicBuilder.name("movie-ingestion-events-dlt")
                .partitions(3) // Matches main topic partitions (0, 1, 2)
                .replicas(1)
                .build();
    }


    @Bean
    public NewTopic movieOutboxEventTopic() {
        return TopicBuilder.name("movie-embedding-events")
                .partitions(3)
                .replicas(1)
                .build();
    }


    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> {
                    log.error("Routing failed Kafka record to DLT: topic={}, partition={}, offset={}, key={}",
                            record.topic(), record.partition(), record.offset(), record.key(), ex);
                    return new TopicPartition(record.topic() + "-dlt", 0);
                }
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        // Optional: Also log each retry attempt at WARN level
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Kafka consumer retry attempt #{} failed for key={}: {}",
                        deliveryAttempt, record.key(), ex.getMessage())
        );

        // BadSqlGrammarException is a code bug, not transient—skip retries and go straight to DLT
        errorHandler.addNotRetryableExceptions(org.springframework.jdbc.BadSqlGrammarException.class);

        return errorHandler;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }


    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .modelName("nomic-embed-text")
                .baseUrl("http://localhost:11434")
                .timeout(Duration.ofSeconds(120))
                .build();
    }
}
