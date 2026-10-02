package org.openl.rules.ruleservice.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ServiceInfoTest {

    @Test
    void exposesTheStartTimeAsADateInMilliseconds() {
        var info = service(Instant.ofEpochSecond(1_721_000_000L, 123_456_789L));

        assertEquals(new Date(1_721_000_000_123L), info.getStartedTime());
    }

    @Test
    void requiresTheStartTime() {
        assertThrows(NullPointerException.class, () -> service(null));
    }

    private static ServiceInfo service(Instant startedTime) {
        return new ServiceInfo(startedTime, "Premium", false, Map.of(), "premium", false, null);
    }
}
