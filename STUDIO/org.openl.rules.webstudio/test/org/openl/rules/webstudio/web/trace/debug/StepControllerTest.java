package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StepControllerTest {

    @Test
    void stopAtEntrySuspendsOnFirstEnter() {
        StepController controller = new StepController();
        controller.armInitial(true);
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null, null, 0));
    }

    @Test
    void runToBreakpointDoesNotStopWithoutBreakpoint() {
        StepController controller = new StepController();
        controller.armInitial(false);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void tableBreakpointStopsOnEnterAtAnyDepth() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("bp"));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 7, "bp", null, null, 0));
        // A table breakpoint matches only on frame entry, not on a sub-step line.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 7, "bp", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void subStepBreakpointStopsOnMatchingCell() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R2C3"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(2, 3), null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(1, 1), null, 0));
        // The same key must not trigger a table-entry stop.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 0));
    }

    @Test
    void instanceIndexedCellBreakpointFiresOnlyOnThatExecution() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R2C3@1"));
        var cell = CurrentLocation.cell(2, 3);
        // Fires on the table's 2nd execution (instance 1), not the 1st or 3rd.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", cell, null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", cell, null, 1));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", cell, null, 2));
    }

    @Test
    void instanceIndexedTableBreakpointFiresOnlyOnThatEnter() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri@3"));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 2));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 3));
    }

    @Test
    void plainCellBreakpointStillFiresOnEveryExecution() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R2C3"));
        var cell = CurrentLocation.cell(2, 3);
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", cell, null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", cell, null, 4));
    }

    @Test
    void ruleFiredBreakpointStopsWhenAnyRuleFires() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#" + CurrentLocation.RULE_FIRED_REF));
        CurrentLocation fired = CurrentLocation.dtRule(List.of("R3"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", fired, "DT", 0));
        // A rule firing in a different table does not match this breakpoint.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "other", fired, "DT", 0));
        // It is a location stop, not a table-entry stop.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, "DT", 0));
    }

    @Test
    void ruleBreakpointStopsOnlyOnTheNamedRule() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R3"));
        // Firing R3 matches the specific-rule breakpoint, but firing another rule does not.
        CurrentLocation r3 = CurrentLocation.dtRule(List.of("R3"));
        CurrentLocation r4 = CurrentLocation.dtRule(List.of("R4"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", r3, "DT", 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", r4, "DT", 0));
    }

    @Test
    void nameBreakpointStopsOnAnySameNamedTable() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("MyRule"));
        // Two versions share the name "MyRule" but have different URIs; the name breakpoint stops on both.
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri/v1", null, "MyRule", 0));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 9, "uri/v2", null, "MyRule", 0));
        // A differently named table is not affected.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 2, "uri/other", null, "Other", 0));
        // The name matches only on entry, not on a sub-step line.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 4, "uri/v1", CurrentLocation.cell(0, 0), "MyRule", 0));
    }

    @Test
    void stepIntoStopsAtNextEventAnyDepth() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 3, "uri", null, null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 9, "uri", CurrentLocation.cell(0, 0), null, 0));
        // The current frame's own exit stops (its returned result is on the stack); a deeper exit does not.
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 2, "uri", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri", null, null, 0));
    }

    @Test
    void stepOverStopsAtCurrentDepthAndOwnExitButRunsThroughCallees() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_OVER, 3);
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", CurrentLocation.cell(0, 0), null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 2, "uri", CurrentLocation.cell(0, 0), null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 4, "uri", CurrentLocation.cell(0, 0), null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri", null, null, 0));
        // The current frame's own exit stops (returned result on the stack); a deeper callee's exit does not.
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 4, "uri", null, null, 0));
    }

    @Test
    void stepOutStopsAtFrameExitThenAboveCurrentFrame() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_OUT, 3);
        // The current frame runs to completion: its own lines do not stop, but its exit does.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", CurrentLocation.cell(0, 0), null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri", null, null, 0));
        // A deeper callee's exit is skipped; the caller's next line stops.
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 4, "uri", null, null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 2, "uri", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void resumeClearsStepping() {
        StepController controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 1, "uri", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void pauseSuspendsAtNextEvent() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.requestPause();
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void armClearsPendingPause() {
        StepController controller = new StepController();
        controller.requestPause();
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(0, 0), null, 0));
    }
}
