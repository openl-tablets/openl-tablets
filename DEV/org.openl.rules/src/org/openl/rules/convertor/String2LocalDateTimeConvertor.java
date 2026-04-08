package org.openl.rules.convertor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class String2LocalDateTimeConvertor implements IString2DataConvertor<LocalDateTime> {

    private final List<DateTimeFormatter> supportedFormats = new ArrayList<>();

    {
        supportedFormats.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        supportedFormats.add(DateTimeFormatter.ofPattern("M/dd/yyyy H:mm a", Locale.US));
    }

    @Override
    public LocalDateTime parse(String data, String format) {
        // format - ignore this parameter. TODO remove from method
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
        throw new IllegalArgumentException("Cannot convert '%s' to LocalDateTime type".formatted(data));
    }

}
