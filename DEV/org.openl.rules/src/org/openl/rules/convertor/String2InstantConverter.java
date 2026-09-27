package org.openl.rules.convertor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class String2InstantConverter implements IString2DataConvertor<Instant> {

    private final List<DateTimeFormatter> supportedFormats = List.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ofPattern("M/dd/yyyy H:mm a VV", Locale.US));

    @Override
    public Instant parse(String data, String format) {
        // format - ignore this parameter
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                return dtFormat.parse(data, Instant::from);
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert '%s' to Instant type".formatted(data));
    }
}
