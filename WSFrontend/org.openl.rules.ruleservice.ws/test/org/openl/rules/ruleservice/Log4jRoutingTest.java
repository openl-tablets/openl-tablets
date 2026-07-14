package org.openl.rules.ruleservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Pins the log4j-to-slf4j routing shared by every unit test.
 *
 * <p>Many modules get {@code log4j-api} transitively (here through Apache POI). The root build puts
 * {@code log4j-to-slf4j} on every test classpath so those calls reach slf4j, and keeps the reverse
 * {@code log4j-slf4j2-impl} bridge off it. Where a packaged webapp also pulls in {@code log4j-core},
 * {@code log4j-to-slf4j} wins by provider priority.
 *
 * <p>The test fails if that wiring breaks.
 *
 * @author Yury Molchan
 */
class Log4jRoutingTest {

    @Test
    void log4jApiRoutesIntoSlf4j() {
        var factory = LogManager.getFactory();
        assertEquals("org.apache.logging.slf4j.SLF4JLoggerContextFactory", factory.getClass().getName());
        // Creating an slf4j logger trips log4j's "log4j-slf4j2-impl cannot be present with log4j-to-slf4j"
        // guard if the conflicting bridge leaks back onto the test classpath, so exercise it too.
        assertDoesNotThrow(() -> LoggerFactory.getLogger(Log4jRoutingTest.class));
    }
}
