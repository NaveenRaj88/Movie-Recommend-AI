package com.movie.dataingester.model;

import lombok.Builder;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
@Builder
public record Movie(long id, String original_language, String original_title, double popularity,
                    LocalDate release_date, double vote_average, long vote_count, String[] genre, String overview, long revenue,
                    int runtime, String tagline) {

    private static final String NULL_SENTINEL = "<NULL>";
    private static final String DELIMITER = "|";

    public static String computeMovieHash(Movie movie) {
        // 1. Build a canonical string of all business fields (exclude audit timestamps)
        String canonicalPayload = new StringJoiner(DELIMITER)
                .add(Objects.toString(movie.id(), NULL_SENTINEL))
                .add(Objects.toString(movie.original_language(), NULL_SENTINEL))
                .add(Objects.toString(movie.original_title(), NULL_SENTINEL))
                .add(Objects.toString(movie.popularity(), NULL_SENTINEL))
                .add(Objects.toString(movie.release_date(), NULL_SENTINEL))
                .add(Objects.toString(movie.vote_average(), NULL_SENTINEL))
                .add(Objects.toString(movie.vote_count(), NULL_SENTINEL))
                .add(movie.genre() != null ? String.join(",", movie.genre()) : NULL_SENTINEL)
                .add(Objects.toString(movie.overview(), NULL_SENTINEL))
                .add(Objects.toString(movie.revenue(), NULL_SENTINEL))
                .add(Objects.toString(movie.runtime(), NULL_SENTINEL))
                .add(Objects.toString(movie.tagline(), NULL_SENTINEL))
                .toString();

        // 2. Hash with SHA-256 and format as a 64-char lowercase hex string
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes); // Exactly 64 characters
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }

    public static String computeEmbeddingHash(Movie movie) {
        // 1. Build a canonical string of all business fields (exclude audit timestamps)
        String canonicalPayload = new StringJoiner(DELIMITER)
                .add(Objects.toString(movie.original_language(), NULL_SENTINEL))
                .add(Objects.toString(movie.original_title(), NULL_SENTINEL))
                .add(movie.genre() != null ? String.join(",", movie.genre()) : NULL_SENTINEL)
                .add(Objects.toString(movie.overview(), NULL_SENTINEL))
                .add(Objects.toString(movie.tagline(), NULL_SENTINEL))
                .toString();

        // 2. Hash with SHA-256 and format as a 64-char lowercase hex string
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes); // Exactly 64 characters
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }


}
