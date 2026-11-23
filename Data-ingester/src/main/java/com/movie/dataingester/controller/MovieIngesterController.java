package com.movie.dataingester.controller;

import com.movie.dataingester.dao.MovieJdbcDao;
import com.movie.dataingester.model.Movie;
import com.movie.dataingester.utils.RecordUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

import static com.movie.dataingester.utils.RecordUtils.parseDateOrNull;

@Slf4j
@RestController()
public class MovieIngesterController {

    @Autowired
    private MovieJdbcDao movieJdbcDao;


    CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setAllowMissingColumnNames(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build();


    @GetMapping("/batchIngest")
    public ResponseEntity<String> batchIngest(@RequestParam(defaultValue = "100") int batchSize) throws IOException {
        //read csv in a batch of 100 into a list
        List<Map<Long, Movie>> batch = readCsvBatch(batchSize);
        movieJdbcDao.saveAllBatches(batch);
        return ResponseEntity.ok("Batch ingest completed successfully.");
    }

    private List<Map<Long, Movie>> readCsvBatch(int batchSize) {
        URL fileUrl = getClass().getResource("/static/Top_10000_Movies.csv");
        List<Map<Long, Movie>> batchList = List.of();

        try (CSVParser csvParser = CSVParser.parse(Path.of(fileUrl.toURI()), StandardCharsets.UTF_8, format)) {
            batchList = csvParser.stream()
                    .filter(csvRecord -> {
                        if(!csvRecord.isConsistent()){
                            log.warn("Inconsistent record: {}", csvRecord);
                            return false;
                        }
                        return true;
                    })
                    .map(this::parseLine)
                    .gather(Gatherers.windowFixed(batchSize))
                    .map(batch -> batch.stream().collect(Collectors.toMap(Movie::id, movie -> movie)))
                    .toList();

        } catch (IOException | URISyntaxException e) {
            System.out.println("Error occurred while reading CSV file: " + e.getMessage() + e);
        }
        return batchList;
    }


    @GetMapping("/streamingIngest")
    public ResponseEntity<String> streamingIngest(@RequestParam(required = true) String fileName){


    }

    private Movie parseLine(CSVRecord csvRecord) {
        long id = RecordUtils.parseLongOrNull(csvRecord.get("id"));
        String original_language = RecordUtils.emptyToNull(csvRecord.get("original_language"));
        String original_title = RecordUtils.emptyToNull(csvRecord.get("original_title"));
        Double popularity = RecordUtils.parseDoubleOrNull(csvRecord.get("popularity"));
        LocalDate release_date = parseDateOrNull(csvRecord.get("release_date"));
        Double vote_average = RecordUtils.parseDoubleOrNull(csvRecord.get("vote_average"));
        long vote_count = RecordUtils.parseIntOrNull(csvRecord.get("vote_count"));
        String[] genre = RecordUtils.parseStringToArray(csvRecord.get("genre"));
        String overview = RecordUtils.emptyToNull(csvRecord.get("overview"));
        long revenue = RecordUtils.parseLongOrNull(csvRecord.get("revenue"));
        int runtime = RecordUtils.parseIntOrNull(csvRecord.get("runtime"));
        String tagline = RecordUtils.emptyToNull(csvRecord.get("tagline"));

        Movie movie = new Movie(id, original_language, original_title, popularity, release_date,
                vote_average, vote_count, genre, overview, revenue, runtime, tagline);

        if (!isValid()) {
            throw new IllegalArgumentException("Invalid movie data");
        }

        return movie;
    }

    private boolean isValid() {
        // Add validation logic here
        return true;
    }


}
