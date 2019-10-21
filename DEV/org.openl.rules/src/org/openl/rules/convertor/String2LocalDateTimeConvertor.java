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
        //format - ignore this parameter. TODO remove from method
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(data, dtFormat);
                return localDateTime;
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert '" + data + "' to LocalDateTime type");
    }

}