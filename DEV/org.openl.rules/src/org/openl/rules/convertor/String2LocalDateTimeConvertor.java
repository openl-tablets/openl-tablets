package org.openl.rules.convertor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class String2LocalDateTimeConvertor implements IString2DataConvertor<LocalDateTime> {

    private final List<DateTimeFormatter> supportedFormats = new ArrayList<>();
    {
        supportedFormats.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        supportedFormats.add(DateTimeFormatter.ofPattern("M/dd/yyyy H:mm a"));
    }

    @Override
    public LocalDateTime parse(String data, String format) {
        if (data == null) {
            return null;
        }
        if (format != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
            try {
                return LocalDateTime.parse(data, df);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                    "Cannot convert \"" + data + "\" to LocalDateTime type using: \"" + format + "\" format");
            }
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(data, dtFormat);
                return localDateTime;
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert \"" + data + "\" to LocalDateTime type");
    }

}