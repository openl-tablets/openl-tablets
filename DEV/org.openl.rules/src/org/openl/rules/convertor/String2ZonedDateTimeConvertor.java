package org.openl.rules.convertor;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

public class String2ZonedDateTimeConvertor implements IString2DataConvertor<ZonedDateTime> {

    private final List<DateTimeFormatter> supportedFormats = List.of(
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("M/d/uuuu h:mm a VV", Locale.US).withResolverStyle(ResolverStyle.STRICT));

    @Override
    public @Nullable ZonedDateTime parse(String data, String format) {
        // format - ignore this parameter
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                return ZonedDateTime.parse(data, dtFormat);
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert '%s' to ZonedDateTime type".formatted(data));
    }

}
