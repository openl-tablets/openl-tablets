package org.openl.rules.ruleservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

/**
 * Pins the logging wiring of the test classpath.
 *
 * <p>The classpath has {@code log4j-api} (via Apache POI) but no {@code log4j-core}, so log4j calls must be
 * routed into slf4j by {@code log4j-to-slf4j}. Otherwise log4j falls back to its own console stub and the
 * events bypass the logger of the invoking tool.
 *
 * @author Yury Molchan
 */
class Log4jRoutingTest {

    @Test
    void log4jApiRoutesIntoSlf4j() {
        var factory = LogManager.getFactory();
        assertEquals("org.apache.logging.slf4j.SLF4JLoggerContextFactory", factory.getClass().getName());
    }
}
