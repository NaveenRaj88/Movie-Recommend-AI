package com.movie.dataingester.model;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public record Movie(long id, String original_language, String original_title, double popularity,
                    LocalDate release_date, double vote_average, long vote_count, String[] genre, String overview, long revenue,
                    int runtime, String tagline) {

}
