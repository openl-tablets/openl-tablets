package org.openl.studio.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultTimeZone;

class DateTimesTest {

    private static final Date MODIFIED_AT = new Date(1_721_000_000_123L);

    @Test
    void atUtcKeepsTheInstantWithMillisecondsInUtc() {
        assertEquals(ZonedDateTime.parse("2024-07-14T23:33:20.123Z"), DateTimes.atUtc(MODIFIED_AT));
    }

    @Test
    void atUtcGivesNullForNoDate() {
        assertNull(DateTimes.atUtc(null));
    }

    @Test
    @DefaultTimeZone("Europe/Berlin")
    void atSystemZoneKeepsTheInstantInTheDefaultZone() {
        assertEquals(ZonedDateTime.parse("2024-07-15T01:33:20.123+02:00[Europe/Berlin]"),
                DateTimes.atSystemZone(MODIFIED_AT));
    }
}
