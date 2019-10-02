package org.openl.rules.convertor;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class String2ZonedDateTimeConvertor implements IString2DataConvertor<ZonedDateTime> {

    private final List<DateTimeFormatter> supportedFormats = new ArrayList<>();
    {
        supportedFormats.add(DateTimeFormatter.ISO_DATE_TIME);
        supportedFormats.add(DateTimeFormatter.ofPattern("M/dd/yyyy H:mm a VV"));
    }

    @Override
    public ZonedDateTime parse(String data, String format) {
        if (data == null) {
            return null;
        }
        if (format != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
            try {
                return ZonedDateTime.parse(data, df);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                    "Cannot convert \"" + data + "\" to ZonedDateTime type using: \"" + format + "\" format");
            }
        }
        for (DateTimeFormatter dtFormat : supportedFormats) {
            try {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(data, dtFormat);
                return zonedDateTime;
            } catch (DateTimeParseException e) {
                // Loop on
            }
        }
        throw new IllegalArgumentException("Cannot convert \"" + data + "\" to ZonedDateTime type");
    }

}
