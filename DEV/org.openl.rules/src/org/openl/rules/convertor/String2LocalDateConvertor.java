package org.openl.rules.convertor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

class String2LocalDateConvertor implements IString2DataConvertor<LocalDate> {

    private final List<DateTimeFormatter> supportedFormats = new ArrayList<>();
    {
        supportedFormats.add(DateTimeFormatter.ISO_LOCAL_DATE);
        supportedFormats.add(DateTimeFormatter.ofPattern("M/dd/yyyy"));
    }

    @Override
    public LocalDate parse(String data, String format) {
        //format - ignore this parameter. TODO remove from method
        if (data == null) {
            return null;
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                LocalDate localDate = LocalDate.parse(data, dtFormat);
                return localDate;
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert '" + data + "' to LocalDate type");
    }

}
