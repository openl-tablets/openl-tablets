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
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null, null));
    }

    @Test
    void runToBreakpointDoesNotStopWithoutBreakpoint() {
        StepController controller = new StepController();
        controller.armInitial(false);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null, null));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", "R0C0", null));
    }

    @Test
    void tableBreakpointStopsOnEnterAtAnyDepth() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("bp"));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 7, "bp", null, null));
        // A table breakpoint matches only on frame entry, not on a sub-step line.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 7, "bp", "R0C0", null));
    }

    @Test
    void subStepBreakpointStopsOnMatchingCell() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R2C3"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R2C3", null));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R1C1", null));
        // The same key must not trigger a table-entry stop.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null));
    }

    @Test
    void nameBreakpointStopsOnAnySameNamedTable() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("MyRule"));
        // Two versions share the name "MyRule" but have different URIs; the name breakpoint stops on both.
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri/v1", null, "MyRule"));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 9, "uri/v2", null, "MyRule"));
        // A differently named table is not affected.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 2, "uri/other", null, "Other"));
        // The name matches only on entry, not on a sub-step line.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 4, "uri/v1", "R0C0", "MyRule"));
    }

    @Test
    void stepIntoStopsAtNextEventAnyDepth() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 3, "uri", null, null));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 9, "uri", "R0C0", null));
        // Step Into lands on the next enter/line, not on a frame exit.
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri", null, null));
    }

    @Test
    void stepOverStopsAtCurrentDepthButRunsThroughCallees() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_OVER, 3);
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", "R0C0", null));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 2, "uri", "R0C0", null));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 4, "uri", "R0C0", null));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri", null, null));
        // Step Over lands in the caller via its next line, not at this frame's own exit.
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri", null, null));
    }

    @Test
    void stepOutStopsAtFrameExitThenAboveCurrentFrame() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_OUT, 3);
        // The current frame runs to completion: its own lines do not stop, but its exit does.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", "R0C0", null));
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri", null, null));
        // A deeper callee's exit is skipped; the caller's next line stops.
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 4, "uri", null, null));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 2, "uri", "R0C0", null));
    }

    @Test
    void resumeClearsStepping() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 1, "uri", "R0C0", null));
    }

    @Test
    void pauseSuspendsAtNextEvent() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.requestPause();
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R0C0", null));
    }

    @Test
    void armClearsPendingPause() {
        StepController controller = new StepController();
        controller.requestPause();
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", "R0C0", null));
    }
}
