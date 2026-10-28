package org.openl.rules.convertor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Reads a {@link LocalDateTime} from a text.
 *
 * <p>The text is a date with a time, in the ISO form {@code 2026-01-01T22:20} or the US form
 * {@code 1/1/2026 10:20 PM}. The US form takes an hour of 1 to 12 with AM or PM.
 *
 * <p>A date without a time is the start of that day: {@code 2026-01-01}, {@code 1/01/2026} and {@code 1/1/2026}
 * are read as {@code 2026-01-01T00:00}. Such a date takes the forms a {@code LocalDate} is read from.
 *
 * <p>A day the month does not have, such as {@code 2/30/2026}, is an error. It is not moved to another day.
 */
public class String2LocalDateTimeConvertor implements IString2DataConvertor<LocalDateTime> {

    private final List<DateTimeFormatter> supportedFormats = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("M/d/uuuu h:mm a", Locale.US).withResolverStyle(ResolverStyle.STRICT));

    private static final String2LocalDateConvertor DATE_CONVERTOR = new String2LocalDateConvertor();

    @Override
    public @Nullable LocalDateTime parse(String data, String format) {
        // format - ignore this parameter
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                return LocalDateTime.parse(data, dtFormat);
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        try {
            return DATE_CONVERTOR.parse(data, format).atStartOfDay();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot convert '%s' to LocalDateTime type".formatted(data), e);
        }
    }

}
