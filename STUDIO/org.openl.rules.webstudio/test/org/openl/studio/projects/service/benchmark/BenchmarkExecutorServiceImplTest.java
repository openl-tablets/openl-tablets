package org.openl.studio.projects.service.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.openl.studio.projects.service.benchmark.BenchmarkExecutorServiceImpl.Measured;

class BenchmarkExecutorServiceImplTest {

    /** Longer than the shortest measurement the benchmark settles for. */
    private static final long LONG_ENOUGH = 4_000_000_000L;

    private static final Measured MEASURED = new Measured("table1", "Main", "Table", true, null);

    @Test
    void measureRepeatedly_endsOnTheFirstRunThatLastsLongEnough() throws InterruptedException {
        var attempts = new AtomicInteger();

        var measurement = BenchmarkExecutorServiceImpl.measureRepeatedly(MEASURED, 3, List.of(), runs -> {
            attempts.incrementAndGet();
            return LONG_ENOUGH;
        });

        assertEquals(1, attempts.get());
        assertEquals(1, measurement.runs());
        assertEquals(LONG_ENOUGH, measurement.executionTime());
        assertEquals(3, measurement.testCases());
        assertEquals("table1", measurement.tableId());
        assertEquals("Table", measurement.name());
        assertTrue(measurement.testTable());
    }

    @Test
    void measureRepeatedly_runsMoreUntilTheMeasurementLastsLongEnough() throws InterruptedException {
        // A run that takes a millisecond: the measurement has to repeat it thousands of times.
        var lastRuns = new AtomicInteger();

        var measurement = BenchmarkExecutorServiceImpl.measureRepeatedly(MEASURED, 1, List.of(), runs -> {
            lastRuns.set(runs);
            return runs * 1_000_000L;
        });

        assertEquals(lastRuns.get(), measurement.runs());
        assertTrue(measurement.runs() > 3000, "expected thousands of runs, got " + measurement.runs());
        assertTrue(measurement.executionTime() > 3_000_000_000L);
    }

    @Test
    void measureRepeatedly_growsAtMostTwoHundredfold() throws InterruptedException {
        var runsAsked = new ArrayList<Integer>();

        BenchmarkExecutorServiceImpl.measureRepeatedly(MEASURED, 1, List.of(), runs -> {
            runsAsked.add(runs);
            // A run that takes no measurable time at all: the number of runs grows by the largest step.
            return runs > 100_000 ? LONG_ENOUGH : 0L;
        });

        assertEquals(List.of(1, 200, 40_000, 8_000_000), runsAsked);
    }

    @Test
    void measureRepeatedly_rulesThatEndAtOnceCannotBeMeasured() {
        // Rules that throw stop at the first run, so no number of runs ever takes any time.
        var error = assertThrows(IllegalStateException.class,
                () -> BenchmarkExecutorServiceImpl.measureRepeatedly(MEASURED, 1, List.of(), runs -> 0L));

        assertTrue(error.getMessage().contains("Table"));
    }

    @Test
    void measureRepeatedly_cancelled() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class,
                    () -> BenchmarkExecutorServiceImpl.measureRepeatedly(MEASURED, 1, List.of(), runs -> LONG_ENOUGH));
        } finally {
            Thread.interrupted();
        }
    }
}
