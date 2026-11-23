package com.movie.dataingester.dao;


import com.movie.dataingester.constants.SQlQueries;
import com.movie.dataingester.model.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class MovieJdbcDao {


    private final JdbcTemplate jdbcTemplate;


    public MovieJdbcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int saveAllBatches(List<Map<Long, Movie>> batchList) {
        int totalInserted = 0;
        int batchInserted;
        for(Map<Long, Movie> batch : batchList) {
            batchInserted = 0;
            List<Object[]> moviesBatch = batch.values().stream().map(movie -> new Object[]{
                    movie.id(),
                    movie.original_language(),
                    movie.original_title(),
                    movie.popularity(),
                    movie.release_date(),
                    movie.vote_average(),
                    movie.vote_count(),
                    // MUST be a String (not movie.genre() String[]!) for string_to_array(?, ',')
                    (movie.genre() != null && movie.genre().length > 0)
                            ? String.join(",", movie.genre())
                            : null,
                    movie.overview(),
                    movie.revenue(),
                    movie.runtime(),
                    movie.tagline()
            }).toList();


            jdbcTemplate.batchUpdate(SQlQueries.insertMovieQuery, moviesBatch);
            log.info("Inserted batch of {} movies into the database.", moviesBatch.size());
            totalInserted += moviesBatch.size();
        }

        return totalInserted;
    }

}
