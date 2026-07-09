package org.openl.studio.common.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Helpers for presenting repository timestamps in the server's local time zone.
 */
public final class DateTimes {

    private DateTimes() {
    }

    /** Convert an instant to a zoned date-time in the system default zone. */
    public static ZonedDateTime atSystemZone(Instant instant) {
        return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /** Convert a legacy {@link Date} to a zoned date-time in the system default zone. */
    public static ZonedDateTime atSystemZone(Date date) {
        return atSystemZone(date.toInstant());
    }
}
