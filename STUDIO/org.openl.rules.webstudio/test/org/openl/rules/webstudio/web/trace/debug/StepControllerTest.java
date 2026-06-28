package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class StepControllerTest {

    @Test
    void stopAtEntrySuspendsOnFirstEnter() {
        StepController controller = new StepController();
        controller.armInitial(true);
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null));
    }

    @Test
    void runToBreakpointDoesNotStopWithoutBreakpoint() {
        StepController controller = new StepController();
        controller.armInitial(false);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", "R0C0"));
    }

    @Test
    void tableBreakpointStopsOnEnterAtAnyDepth() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("bp"));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 7, "bp", null));
        // A table breakpoint matches only on frame entry, not on a sub-step line.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 7, "bp", "R0C0"));
    }

    @Test
    void subStepBreakpointStopsOnMatchingCell() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R2C3"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R2C3"));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R1C1"));
        // The same key must not trigger a table-entry stop.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null));
    }

    @Test
    void stepIntoStopsAtNextEventAnyDepth() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 3, "uri", null));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 9, "uri", "R0C0"));
    }

    @Test
    void stepOverStopsAtCurrentDepthButRunsThroughCallees() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_OVER, 3);
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", "R0C0"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 2, "uri", "R0C0"));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 4, "uri", "R0C0"));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri", null));
    }

    @Test
    void stepOutStopsOnlyAboveCurrentFrame() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_OUT, 3);
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", "R0C0"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 2, "uri", "R0C0"));
    }

    @Test
    void resumeClearsStepping() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 1, "uri", "R0C0"));
    }

    @Test
    void pauseSuspendsAtNextEvent() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.requestPause();
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R0C0"));
    }

    @Test
    void armClearsPendingPause() {
        StepController controller = new StepController();
        controller.requestPause();
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R0C0"));
    }
}
