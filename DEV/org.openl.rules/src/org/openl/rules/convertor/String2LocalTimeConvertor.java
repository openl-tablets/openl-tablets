package org.openl.rules.convertor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class String2LocalTimeConvertor implements IString2DataConvertor<LocalTime> {

    private final List<DateTimeFormatter> supportedFormats = new ArrayList<>();

    {
        supportedFormats.add(DateTimeFormatter.ISO_LOCAL_TIME);
        supportedFormats.add(DateTimeFormatter.ofPattern("H:mm a", Locale.US));
        supportedFormats.add(DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
    }

    @Override
    public LocalTime parse(String data, String format) {
        // format - ignore this parameter. TODO remove from method
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
