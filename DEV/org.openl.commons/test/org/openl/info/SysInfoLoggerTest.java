package org.openl.info;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;
import org.junitpioneer.jupiter.DefaultTimeZone;
import org.junitpioneer.jupiter.StdErr;
import org.junitpioneer.jupiter.StdIo;

class SysInfoLoggerTest {

    private static final Pattern TIME_LINE = Pattern.compile(
            "Time : \\d{4}-\\d{2}-\\d{2} {3}\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\+0[12]:00 \\(CES?T\\) \\(Europe/Berlin - ");

    @Test
    @StdIo
    @DefaultTimeZone("Europe/Berlin")
    @DefaultLocale("en-US")
    void logsTheCurrentTimeWithMillisecondsOffsetAndZoneName(StdErr err) {
        new SysInfoLogger().log();

        var timeLine = Arrays.stream(err.capturedLines())
                .filter(line -> line.contains("Time : "))
                .findFirst()
                .orElseThrow();
        assertTrue(TIME_LINE.matcher(timeLine).find(), timeLine);
    }
}
