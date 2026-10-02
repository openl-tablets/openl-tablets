package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.testmethod.TestUnitsResults;

class JUnitReportWriterTest {

    private static final Pattern TIMESTAMP = Pattern.compile(
            "timestamp=\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})\"");

    @TempDir
    Path dir;

    @Test
    void stampsTheSuiteWithTheLocalDateTimeInSeconds() throws Exception {
        var result = mock(TestUnitsResults.class, RETURNS_DEEP_STUBS);
        when(result.getTestSuite().getTestSuiteMethod().getName()).thenReturn("PremiumTest");
        when(result.getTestSuite().getTestSuiteMethod().getModuleName()).thenReturn("Main");
        when(result.getTestUnits()).thenReturn(List.of());

        var before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        new JUnitReportWriter(dir.toFile()).write(result);
        var after = LocalDateTime.now();

        var report = Files.readString(dir.resolve("TEST-OpenL.Main.PremiumTest.xml"));
        var matcher = TIMESTAMP.matcher(report);
        assertTrue(matcher.find(), report);
        var timestamp = LocalDateTime.parse(matcher.group(1));
        assertFalse(timestamp.isBefore(before), timestamp + " is before " + before);
        assertFalse(timestamp.isAfter(after), timestamp + " is after " + after);
    }
}
