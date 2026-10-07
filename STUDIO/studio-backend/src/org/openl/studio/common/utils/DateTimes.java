package org.openl.studio.common.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.jspecify.annotations.Nullable;

/**
 * Helpers for presenting repository timestamps as zoned date-times.
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

    /** Convert a legacy {@link Date} to a date-time in UTC; a {@code null} date gives {@code null}. */
    public static @Nullable ZonedDateTime atUtc(@Nullable Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneOffset.UTC);
    }
}
