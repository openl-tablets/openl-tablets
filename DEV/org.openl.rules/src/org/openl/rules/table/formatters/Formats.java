package org.openl.rules.table.formatters;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public interface Formats {
    String date();

    String dateTime();

    default String formatDate(Date date) {
        return new SimpleDateFormat(date()).format(date);
    }

    default String formatDateTime(Date date) {
        return new SimpleDateFormat(dateTime()).format(date);
    }

    default String formatDateOrDateTime(Date date) {
        // Check whether the date contains a time other than the default
        LocalDateTime dateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        if (dateTime.getHour() == 0 && dateTime.getMinute() == 0) {
            return formatDate(date);
        } else {
            return formatDateTime(date);
        }
    }
}
