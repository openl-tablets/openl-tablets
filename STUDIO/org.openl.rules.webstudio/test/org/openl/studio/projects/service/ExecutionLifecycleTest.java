package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

/**
 * What a caller is told about a task that was run for them, and what they are given afterwards.
 */
class ExecutionLifecycleTest {

    /** Remembers what the listener was told, in the order it was told. */
    private static final class Reported implements ExecutionProgressListener {

        private final List<ExecutionStatus> statuses = new ArrayList<>();
        private String error;

        @Override
        public void onStatusChanged(ExecutionStatus status) {
            statuses.add(status);
        }

        @Override
        public void onError(String message, Throwable cause) {
            error = message;
        }
    }

    @Test
    void tellsTheListenerThatTheTaskRanAndGivesItsResult() throws Exception {
        var reported = new Reported();

        var result = ExecutionLifecycle.execute(reported, () -> "done");

        assertEquals("done", result.get());
        assertEquals(List.of(ExecutionStatus.STARTED, ExecutionStatus.COMPLETED), reported.statuses);
    }

    @Test
    void givesTheResultOfATaskThatFinishedWhileItWasAskedToStop() throws Exception {
        var reported = new Reported();

        var result = ExecutionLifecycle.execute(reported, () -> {
            Thread.currentThread().interrupt();
            return "all of it";
        });

        // The task reached its end, so what it found is whole and is handed over; the listener still
        // hears that the work was asked to stop.
        assertEquals("all of it", result.get());
        assertEquals(List.of(ExecutionStatus.STARTED, ExecutionStatus.INTERRUPTED), reported.statuses);
        assertTrue(Thread.interrupted(), "the interrupt is left for the thread that owns it");
    }

    @Test
    void offersNoResultOfATaskThatGaveUpOnTheInterruption() throws Exception {
        var reported = new Reported();

        var result = ExecutionLifecycle.execute(reported, () -> {
            throw new IllegalStateException(new InterruptedException());
        });

        assertNull(result.get());
        assertEquals(List.of(ExecutionStatus.STARTED, ExecutionStatus.INTERRUPTED), reported.statuses);
        assertNull(reported.error, "an interruption is not a failure to report");
    }

    @Test
    void namesAFailureThatSaysNothingByWhatItIs() {
        var reported = new Reported();

        var result = ExecutionLifecycle.execute(reported, () -> {
            throw new IllegalStateException();
        });

        assertThrows(ExecutionException.class, result::get);
        assertEquals(List.of(ExecutionStatus.STARTED), reported.statuses);
        assertEquals("IllegalStateException", reported.error);
    }
}
