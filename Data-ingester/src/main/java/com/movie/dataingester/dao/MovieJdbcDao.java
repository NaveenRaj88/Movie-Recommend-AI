package com.movie.dataingester.dao;


import com.movie.dataingester.constants.SQlQueries;
import com.movie.dataingester.model.*;
import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Repository
public class MovieJdbcDao {


    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public MovieJdbcDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int saveAllBatches(List<Map<Long, Movie>> batchList) {
        int totalInserted = 0;
        int batchInserted;
        for (Map<Long, Movie> batch : batchList) {
            batchInserted = 0;
            List<Movie> moviesBatch = batch.values().stream().toList();
            jdbcTemplate.batchUpdate(SQlQueries.insertMovieQuery, moviesBatch, 500, (PreparedStatement ps, Movie movie) -> {
                ps.setLong(1, movie.id());
                ps.setString(2, movie.original_language());
                ps.setString(3, movie.original_title());
                ps.setDouble(4, movie.popularity());
                ps.setObject(5, movie.release_date()); // Safely handles LocalDate and null
                ps.setDouble(6, movie.vote_average());
                ps.setLong(7, movie.vote_count());
                ps.setString(8, movie.genre() != null ? String.join(",", movie.genre()) : null);
                ps.setString(9, movie.overview());
                ps.setLong(10, movie.revenue());
                ps.setInt(11, movie.runtime());
                ps.setString(12, movie.tagline());
                ps.setString(13, Movie.computeMovieHash(movie));
            });
            Map<Long, MovieEmbeddings> movieEmbeddingsMap = getMovieEmbeddingRecords(moviesBatch.stream().map(Movie::id).toList());
            List<OutboxEvent> eligibleEmbeddingEvents = getEligibleEmbeddingEvents(movieEmbeddingsMap, moviesBatch);
            saveOutboxEvents(eligibleEmbeddingEvents);
            log.info("Inserted batch of {} movies into the database.", moviesBatch.size());
            totalInserted += moviesBatch.size();
        }
        return totalInserted;
    }


    private List<OutboxEvent> getEligibleEmbeddingEvents(Map<Long, MovieEmbeddings> movieEmbeddingsMap, List<Movie> movies) {
        List<OutboxEvent> outboxEventList = new ArrayList<>();
        movies.stream().forEach(m ->
                {
                    if (movieEmbeddingsMap.containsKey(m.id()) &&
                            movieEmbeddingsMap.get(m.id()).contentHash().equals(Movie.computeEmbeddingHash(m))) {
                        log.info("Movie id{} embedding hash are equal so skipping the update", m.id());
                    } else {

                        OutboxEventPayload payload = new OutboxEventPayload(m.id(), Movie.computeEmbeddingHash(m));

                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .aggregateType("Movie")
                                .aggregateId(m.id())
                                .eventType("MovieEmbeddingRequest")
                                .status(OutboxStatusEnum.PUBLISHED)
                                .attempts(0)
                                .payload(objectMapper.writeValueAsString(payload))
                                .build();
                        outboxEventList.add(outboxEvent);
                    }
                }
        );
        return outboxEventList;
    }

    private int saveOutboxEvents(List<OutboxEvent> outboxEventList) {
        if (outboxEventList.isEmpty()) return 0;
        int[][] result = jdbcTemplate.batchUpdate(SQlQueries.insertOutboxEventsQuery, outboxEventList, 500, (PreparedStatement ps, OutboxEvent o) ->
        {
            //id,aggregate_type ,aggregate_id, event_type, payload,status,attempts,created_at, published_at,last_error
            ps.setString(1, o.aggregateType());
            ps.setLong(2, o.aggregateId());
            ps.setString(3, o.eventType());
            ps.setString(4, o.payload());
        });
        log.info("{} inserted to the outbox events", result.length);
        return result.length;
    }


    @Transactional
    public int saveMovieEmbeddings(List<MovieEmbeddings> movieEmbeddingsList) {
        int[][] resultArray = new int[0][0];
        if (movieEmbeddingsList != null && !movieEmbeddingsList.isEmpty()) {
            resultArray = jdbcTemplate.batchUpdate(SQlQueries.insertMovieEmbeddingsQuery, movieEmbeddingsList, 500, (PreparedStatement ps, MovieEmbeddings me) ->
            {
                ps.setLong(1, me.movieId());
                ps.setObject(2, new PGvector(me.embedding()));
                ps.setString(3, me.modelName());
                ps.setString(4, me.contentHash());
                ps.setObject(5, me.embeddedAt());
            });
            log.info("Inserted batch of {} movie embeddings into the database.", movieEmbeddingsList.size());
        }
        return resultArray.length;
    }


    private Map<Long, MovieEmbeddings> getMovieEmbeddingRecords(List<Long> movieIds) {
        Map<Long, MovieEmbeddings> resultMap = Collections.emptyMap();
        jdbcTemplate.query(SQlQueries.selectMovieEmbeddingsQuery,
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("bigint", movieIds.toArray(Long[]::new))),
                rs -> {
                    long movieId = rs.getLong("movie_id");
                    float[] embedding = (float[]) rs.getArray("embedding").getArray();
                    String modelName = rs.getString("model_name");
                    String contentHash = rs.getString("content_hash");
                    String status = rs.getString("status");
                    Integer attemptCount = rs.getInt("attempt_count");
                    String lastError = rs.getString("last_error");
                    LocalDateTime embeddedAt = rs.getTimestamp("embedded_at") != null ? rs.getTimestamp("embedded_at").toLocalDateTime() : null;
                    LocalDateTime createdAt = rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null;
                    LocalDateTime updatedAt = rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null;

                    MovieEmbeddings movieEmbeddings = new MovieEmbeddings(
                            movieId,
                            embedding,
                            modelName,
                            contentHash,
                            status,
                            attemptCount,
                            lastError,
                            embeddedAt,
                            createdAt,
                            updatedAt
                    );
                    resultMap.put(movieId, movieEmbeddings);
                });
        return resultMap;
    }

    @Transactional
    public Map<Long, Movie> getMovieRecords(List<Long> movieIds) {
        Map<Long, Movie> movies = new HashMap<>();
        jdbcTemplate.query(SQlQueries.selectMovieRecords,
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("bigint", movieIds.toArray(Long[]::new))),
                rs -> {
                    long movieId = rs.getLong("id");
                    String original_language = rs.getString("original_language");
                    String original_title = rs.getString("original_title");
                    String[] genre = (String[]) rs.getArray("genre").getArray();
                    String overview = rs.getString("overview");
                    String tagline = rs.getString("tagline");

                    Movie movie = Movie.builder()
                            .id(movieId)
                            .original_language(original_language)
                            .original_title(original_title)
                            .genre(genre)
                            .overview(overview)
                            .tagline(tagline)
                            .build();

                    movies.put(movieId, movie);

                });
        return movies;
    }

    public Map<Long, OutboxEvent> getMovieOutBoxEvents(List<Long> ids) {
        Map<Long, OutboxEvent> outboxEventMap = new HashMap<>();
        jdbcTemplate.query(SQlQueries.selectMovieOutBoxEvents,
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("bigint", ids.toArray(Long[]::new))),
                rs -> {

                    OutboxEvent outboxEvent = OutboxEvent.builder()
                            .id(rs.getLong("id"))
                            .aggregateType(rs.getString("aggregate_type"))
                            .aggregateId(rs.getLong("aggregate_id"))
                            .eventType(rs.getString("event_type"))
                            .payload(rs.getString("payload"))
                            .status(OutboxStatusEnum.valueOf(rs.getString("status")))
                            .attempts(rs.getInt("attempts"))
                            .createdAt(rs.getObject(rs.getString("created_at"), LocalDateTime.class))
                            .publishedAt(rs.getObject(rs.getString("published_at"), LocalDateTime.class))
                            .lastError(rs.getString("last_error"))
                            .build();
                    outboxEventMap.put(outboxEvent.id(), outboxEvent);

                });
        return outboxEventMap;
    }

    public Map<Long, OutboxEvent> getMovieOutboxEventsBatch() {
        Map<Long, OutboxEvent> outboxEventMap = new HashMap<>();
        jdbcTemplate.query(SQlQueries.selectMovieOutBoxEvents,
                rs -> {
                    OutboxEvent outboxEvent = OutboxEvent.builder()
                            .id(rs.getLong("id"))
                            .aggregateType(rs.getString("aggregate_type"))
                            .aggregateId(rs.getLong("aggregate_id"))
                            .eventType(rs.getString("event_type"))
                            .payload(rs.getString("payload"))
                            .status(OutboxStatusEnum.valueOf(rs.getString("status")))
                            .attempts(rs.getInt("attempts"))
                            .createdAt(rs.getObject("created_at", OffsetDateTime.class).toLocalDateTime())
                            .lastError(rs.getString("last_error"))
                            .build();
                    outboxEventMap.put(outboxEvent.id(), outboxEvent);

                });
        return outboxEventMap;
    }

    @Transactional
    public void updateOutboxEventStatus(Long id) {
        jdbcTemplate.update(SQlQueries.updateOutboxEventStatus, ps -> ps.setLong(1, id));
    }
}
