package org.openl.gen.writers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ISO8601DateFormater {
    private static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static String format(Date date) {
        var formatted = new SimpleDateFormat(ISO8601_DATE_FORMAT, Locale.US).format(date);
        return formatted.endsWith("T00:00:00Z") ? formatted.substring(0, formatted.length() - 10) : formatted;
    }
}
