package org.openl.rules.convertor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class String2LocalTimeConvertor implements IString2DataConvertor<LocalTime> {

    private final List<DateTimeFormatter> supportedFormats = new ArrayList<>();
    {
        supportedFormats.add(DateTimeFormatter.ISO_LOCAL_TIME);
        supportedFormats.add(DateTimeFormatter.ofPattern("H:mm a"));
        supportedFormats.add(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    @Override
    public LocalTime parse(String data, String format) {
        //format - ignore this parameter. TODO remove from method
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                LocalTime localTime = LocalTime.parse(data, dtFormat);
                return localTime;
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException(String.format("Cannot convert '%s' to LocalTime type", data));
    }

}
