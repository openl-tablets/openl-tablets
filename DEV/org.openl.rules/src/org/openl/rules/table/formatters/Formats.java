package org.openl.rules.table.formatters;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public interface Formats {
    String date();

    String dateTime();

    /**
     * Format date object to have date info (without time). Note: you should not use it in a loop because date format
     * retrieving can be slow. Instead, you should retrieve format once and use preconfigured SimpleDateFormat in the
     * loop.
     *
     * @see #date()
     */
    default String formatDate(Date date) {
        return new SimpleDateFormat(date()).format(date);
    }

    /**
     * Format date object to have date and time info. Note: you should not use it in a loop because dateTime format
     * retrieving can be slow. Instead, you should retrieve format once and use preconfigured SimpleDateFormat in the
     * loop.
     * 
     * @see #dateTime()
     */
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
