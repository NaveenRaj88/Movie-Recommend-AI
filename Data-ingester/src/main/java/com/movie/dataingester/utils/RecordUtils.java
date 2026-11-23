package com.movie.dataingester.utils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public interface RecordUtils {

    static String emptyToNull(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    static Integer parseIntOrNull(String val) {
        if (val == null || val.isBlank()) return 0;
        return (int) Double.parseDouble(val.trim());
    }

     static Long parseLongOrNull(String val) {
        if (val == null || val.isBlank()) return 0L;
        return (long) Double.parseDouble(val.trim());
    }

    static Double parseDoubleOrNull(String val) {
        if (val == null || val.isBlank()) return null;
        return Double.parseDouble(val.trim());
    }

    static LocalDate parseDateOrNull(String val) {
        if (val == null || val.isBlank()) return null;
        return LocalDate.parse(val.trim());
    }

    static String[] parseStringToArray(String val){
        if (val == null || val.isBlank() || val.equals("[]")) {
            return new String[0];
        }
        String trimmed = val.trim();
        if(trimmed.startsWith("[") && trimmed.endsWith("]"))  {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }
}
