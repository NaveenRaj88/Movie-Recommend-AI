package com.movie.dataingester.constants;

public interface SQlQueries {

    String insertMovieQuery = """
            INSERT INTO movies (
                id, original_language, original_title, popularity, release_date,
                vote_average, vote_count, genre, overview, revenue, runtime, tagline,
                content_hash
            ) VALUES (?, ?, ?, ?, ?, ?, ?, string_to_array(?, ','), ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                original_language = EXCLUDED.original_language,
                original_title    = EXCLUDED.original_title,
                popularity        = EXCLUDED.popularity,
                release_date      = EXCLUDED.release_date,
                vote_average      = EXCLUDED.vote_average,
                vote_count        = EXCLUDED.vote_count,
                genre             = EXCLUDED.genre,
                overview          = EXCLUDED.overview,
                revenue           = EXCLUDED.revenue,
                runtime           = EXCLUDED.runtime,
                tagline           = EXCLUDED.tagline,
                content_hash       = EXCLUDED.content_hash,
                updated_at        = CURRENT_TIMESTAMP
            WHERE movies.content_hash IS DISTINCT FROM EXCLUDED.content_hash;
            """;

    String insertOutboxEventsQuery = """
            INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload )
            VALUES ( ?,?,?,CAST(? AS JSONB))
            """;

    String insertMovieEmbeddingsQuery = """
            INSERT INTO movie_embeddings (
                movie_id, embedding, model_name, content_hash,
                status, attempt_count, last_error, embedded_at,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, 'COMPLETED', 1, NULL, CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (movie_id) DO UPDATE SET
                embedding = EXCLUDED.embedding,
                model_name = EXCLUDED.model_name,
                content_hash = EXCLUDED.content_hash,
                status = 'COMPLETED',
                attempt_count = movie_embeddings.attempt_count + 1,
                last_error = NULL,
                embedded_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP;
            """;

    String selectMovieEmbeddingsQuery = """
            SELECT movie_id, embedding, model_name, content_hash, status, attempt_count, last_error, embedded_at, created_at, updated_at
            FROM movie_embeddings
            WHERE movie_id = ANY (?)
            """;

    String selectMovieRecords = """
            Select id, original_language, original_title, genre, overview, tagline
            from movies where id = ANY(?)
            """;

    String selectMovieOutBoxEvents = """
            SELECT *
            FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY id ASC
            LIMIT 100;
            """;
}
