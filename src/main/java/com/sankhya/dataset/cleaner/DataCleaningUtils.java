package com.sankhya.dataset.cleaner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DataCleaningUtils {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    public static String cleanText(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static BigDecimal parseDecimal(String raw) {
        if (raw == null) return null;
        String clean = raw.trim()
                .replace("₹", "")
                .replace("$", "")
                .replace(",", "")
                .replaceAll("\\s+", "");
        if (clean.isEmpty()) return null;
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal cleanDecimal(String raw) {
        return parseDecimal(raw);
    }

    public static Integer parseInteger(String raw) {
        if (raw == null) return null;
        String clean = raw.trim()
                .replace(",", "")
                .replaceAll("\\s+", "");
        if (clean.isEmpty()) return null;
        // In case decimal like "10.0" comes from excel
        if (clean.contains(".")) {
            clean = clean.substring(0, clean.indexOf('.'));
        }
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer cleanInteger(String raw) {
        return parseInteger(raw);
    }

    public static LocalDateTime parseDateTime(String raw) {
        if (raw == null) return null;
        String clean = raw.trim();
        if (clean.isEmpty()) return null;

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(clean, formatter);
            } catch (DateTimeParseException ignored) {}
        }

        // Try date only and append start of day
        LocalDate date = parseDate(clean);
        if (date != null) {
            return date.atStartOfDay();
        }

        return null;
    }

    public static LocalDateTime cleanDateTime(String raw) {
        return parseDateTime(raw);
    }

    public static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String clean = raw.trim();
        if (clean.isEmpty()) return null;

        // Strip time if included
        if (clean.contains(" ")) {
            clean = clean.split(" ")[0];
        } else if (clean.contains("T")) {
            clean = clean.split("T")[0];
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, formatter);
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    public static LocalDate cleanDate(String raw) {
        return parseDate(raw);
    }

    public static String normalizePaymentMethod(String raw) {
        if (raw == null) return "Cash";
        String lower = raw.trim().toLowerCase();
        if (lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm")) {
            return "UPI";
        }
        if (lower.contains("card") || lower.contains("credit") || lower.contains("debit") || lower.contains("pos")) {
            return "Card";
        }
        if (lower.contains("net") || lower.contains("bank") || lower.contains("neft") || lower.contains("rtgs")) {
            return "Net Banking";
        }
        if (lower.contains("cash")) {
            return "Cash";
        }
        return cleanText(raw);
    }
}
