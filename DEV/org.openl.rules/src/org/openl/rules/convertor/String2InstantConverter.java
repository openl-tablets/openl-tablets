package org.openl.rules.convertor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class String2InstantConverter implements IString2DataConvertor<Instant> {

    private final List<DateTimeFormatter> supportedFormats = new ArrayList<>();

    {
        supportedFormats.add(DateTimeFormatter.ISO_INSTANT);
        supportedFormats.add(DateTimeFormatter.ofPattern("M/dd/yyyy H:mm a VV"));
    }

    @Override
    public Instant parse(String data, String format) {
        // format - ignore this parameter. TODO remove from method
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                Instant instantDate = dtFormat.parse(data, Instant::from);
                return instantDate;
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException(String.format("Cannot convert '%s' to Instant type", data));
    }
}
