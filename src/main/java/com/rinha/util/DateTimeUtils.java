package com.rinha.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

public class DateTimeUtils {

    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .optionalStart().appendOffsetId().optionalEnd()
        .optionalStart().appendLiteral('Z').optionalEnd()
        .toFormatter();

    public static Instant parseToInstant(String dateString) {
        try {
            TemporalAccessor ta = FLEXIBLE_FORMATTER.parseBest(dateString, ZonedDateTime::from, LocalDateTime::from);
            if (ta instanceof ZonedDateTime zdt) {
                return zdt.toInstant();
            } else if (ta instanceof LocalDateTime ldt) {
                return ldt.toInstant(ZoneOffset.UTC);
            } else {
                throw new DateTimeParseException("Unknown date-time format", dateString, 0);
            }
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Failed to parse date-time: " + dateString, dateString, 0);
        }
    }
}
