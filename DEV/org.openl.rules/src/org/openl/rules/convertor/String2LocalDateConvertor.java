package org.openl.rules.convertor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

class String2LocalDateConvertor implements IString2DataConvertor<LocalDate> {

    private final List<DateTimeFormatter> supportedFormats = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT));

    @Override
    public @Nullable LocalDate parse(String data, String format) {
        // format - ignore this parameter
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                return LocalDate.parse(data, dtFormat);
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert '%s' to LocalDate type".formatted(data));
    }

}
