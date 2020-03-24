package org.openl.gen.writers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ISO8601DateFormater {
    private static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String ISO8601_SHORT_DATE_FORMAT = "yyyy-MM-dd";

    public static String format(Date date) {
        SimpleDateFormat dateFormat = null;
        boolean zeroTime = date.getHours() == 0 && date.getMinutes() == 0 && date.getSeconds() == 0;
        if (!zeroTime) {
            dateFormat = new SimpleDateFormat(ISO8601_DATE_FORMAT, Locale.US);
        } else {
            dateFormat = new SimpleDateFormat(ISO8601_SHORT_DATE_FORMAT, Locale.US);
        }
        return dateFormat.format(date);
    }
}
