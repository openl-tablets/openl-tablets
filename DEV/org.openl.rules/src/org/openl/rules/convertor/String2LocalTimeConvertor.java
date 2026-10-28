package org.openl.rules.convertor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

public class String2LocalTimeConvertor implements IString2DataConvertor<LocalTime> {

    private final List<DateTimeFormatter> supportedFormats = List.of(
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("h:mm a", Locale.US).withResolverStyle(ResolverStyle.STRICT));

    @Override
    public @Nullable LocalTime parse(String data, String format) {
        // format - ignore this parameter
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                return LocalTime.parse(data, dtFormat);
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert '%s' to LocalTime type".formatted(data));
    }

}
