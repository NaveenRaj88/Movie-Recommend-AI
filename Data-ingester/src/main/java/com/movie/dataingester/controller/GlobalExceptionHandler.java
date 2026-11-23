package com.movie.dataingester.controller;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Auto-configured by spring-boot-starter-opentelemetry
    private final Tracer tracer;

    /**
     * 1. Domain / Client Input Errors (400 Bad Request)
     * e.g., Missing CSV file path, invalid batchSize, malformed request parameters
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        // 4xx errors are client mistakes: log at WARN without dumping a noisy stack trace
        log.warn("Bad request on path={}: reason={}", request.getRequestURI(), ex.getMessage());

        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid Request Parameter",
                ex.getMessage(),
                "urn:movie-ingester:error:bad-request",
                request
        );
    }

    /**
     * 2. File / I/O Ingestion Failures (422 Unprocessable Entity or 500)
     */
    @ExceptionHandler(IOException.class)
    public ProblemDetail handleIoException(IOException ex, HttpServletRequest request) {
        log.error("CSV I/O failure on path={}", request.getRequestURI(), ex);

        return buildProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "CSV Processing Error",
                "Failed to read or parse the provided CSV resource.",
                "urn:movie-ingester:error:csv-io-failure",
                request
        );
    }

    /**
     * 3. Database / PostgreSQL Failures (503 Service Unavailable / 500)
     * Translates Spring's DataAccessException without leaking raw SQL or schema names
     */
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDatabaseException(DataAccessException ex, HttpServletRequest request) {
        // Log full root cause internally (ECS JSON captures the stack trace + traceId)
        log.error("Database failure during ingestion on path={}", request.getRequestURI(), ex);

        return buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database Persistence Error",
                "A database error occurred while persisting records. Quote the traceId for support.",
                "urn:movie-ingester:error:database-failure",
                request
        );
    }

    /**
     * 4. Catch-All Fallback for Unexpected Bugs (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled system exception on path={}", request.getRequestURI(), ex);

        return buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Quote the traceId for support.",
                "urn:movie-ingester:error:internal-error",
                request
        );
    }

    private ProblemDetail buildProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            String typeUrn,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(typeUrn));
        problem.setInstance(URI.create(request.getRequestURI()));

        // RFC 9457 allows arbitrary extension properties via setProperty()
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("traceId", getCurrentTraceId());

        return problem;
    }

    private String getCurrentTraceId() {
        return Optional.ofNullable(tracer.currentSpan())
                .map(span -> span.context().traceId())
                .orElse("no-active-trace");
    }
}