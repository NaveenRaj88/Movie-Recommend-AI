package com.movie.dataingester.constants;

public interface SQlQueries {

    String insertMovieQuery = """
                        INSERT INTO movies (id, original_language, original_title, popularity, release_date, vote_average,
                        vote_count, genre, overview, revenue, runtime, tagline) VALUES (?,?,?,?,?,?,?, string_to_array(?, ','), ?,?,?,?)
                       ON CONFLICT (id) DO NOTHING 
                       """;


}
