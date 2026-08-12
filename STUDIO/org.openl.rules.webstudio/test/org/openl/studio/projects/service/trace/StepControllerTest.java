package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class StepControllerTest {

    @Test
    void stopAtEntrySuspendsOnFirstEnter() {
        var controller = new StepController();
        controller.armInitial(true);
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null, null, 0));
    }

    @Test
    void runToBreakpointDoesNotStopWithoutBreakpoint() {
        var controller = new StepController();
        controller.armInitial(false);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 1, "uri", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "uri", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void tableBreakpointStopsOnEnterAtAnyDepth() {
        var controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("bp"));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 7, "bp", null, null, 0));
        // A table breakpoint matches only on frame entry, not on a sub-step line.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 7, "bp", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void subStepBreakpointStopsOnMatchingCell() {
        var controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R2C3"));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(2, 3), null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(1, 1), null, 0));
        // The same key must not trigger a table-entry stop.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 0));
    }

    @Test
    void inclusiveTableBreakpointRunsTheTableAndStopsAtItsOwnExit() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("after:bp@0"));
        // The matched entry arms instead of suspending; everything the table does runs through.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 3, "bp", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 4, "child", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 4, "child", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 3, "bp", CurrentLocation.cell(0, 0), null, 0));
        // The table's own exit is the stop: its parameters and result are both on the stack there.
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 3, "bp", null, null, 0));
        // A later instance of the same table does not re-arm the @0 key.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 3, "bp", null, null, 1));
    }

    @Test
    void inclusiveSubStepBreakpointRunsTheStepAndStopsOnTheNextLine() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("after:uri#R2C3@0"));
        // The matched line arms instead of suspending; the tables the step calls run through.
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(2, 3), null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 6, "called", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 6, "called", null, null, 0));
        // The next line of the owning table is the stop: the step's value is computed by then.
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(2, 4), null, 0));
    }

    @Test
    void inclusiveSubStepBreakpointOnTheLastLineStopsAtTheFrameExit() {
        StepController controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("after:uri#R9C1"));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 2, "uri", CurrentLocation.cell(9, 1), null, 0));
        // No further line follows the last step, so the owning frame's exit is the stop.
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 2, "uri", null, null, 0));
    }

    @Test
    void instanceIndexedCellBreakpointFiresOnlyOnThatExecution() {
        var controller = new StepController();
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
        var controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri@3"));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 2));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 3));
    }

    @Test
    void plainCellBreakpointStillFiresOnEveryExecution() {
        var controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("uri#R2C3"));
        var cell = CurrentLocation.cell(2, 3);
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", cell, null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", cell, null, 4));
    }

    @Test
    void ruleFiredBreakpointStopsWhenAnyRuleFires() {
        var controller = new StepController();
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
        var controller = new StepController();
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
        var controller = new StepController();
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
    void instanceIndexedNameBreakpointFiresOnlyOnThatExecution() {
        var controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("MyRule@2"));
        // An indexed name key selects an execution exactly as an indexed URI key does.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri/v1", null, "MyRule", 1));
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri/v1", null, "MyRule", 2));
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri/v1", null, "MyRule", 3));
        // The index does not leak onto a differently named table.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 4, "uri/other", null, "Other", 2));
    }

    @Test
    void inclusiveInstanceIndexedNameBreakpointRunsThatExecutionAndStopsAtItsExit() {
        var controller = new StepController();
        controller.armInitial(false);
        controller.setBreakpoints(Set.of("after:MyRule@1"));
        // The earlier execution is not the target, so nothing arms and its exit runs through.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 3, "uri/v1", null, "MyRule", 0));
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri/v1", null, "MyRule", 0));
        // The indexed execution arms instead of suspending and stops at its own exit, result on the stack.
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 3, "uri/v1", null, "MyRule", 1));
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri/v1", null, "MyRule", 1));
    }

    @Test
    void stepIntoStopsAtNextEventAnyDepth() {
        var controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        assertTrue(controller.shouldSuspend(DebugEvent.ENTER, 3, "uri", null, null, 0));
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 9, "uri", CurrentLocation.cell(0, 0), null, 0));
        // The current frame's own exit stops (its returned result is on the stack); a deeper exit does not.
        assertTrue(controller.shouldSuspend(DebugEvent.EXIT, 2, "uri", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.EXIT, 3, "uri", null, null, 0));
    }

    @Test
    void stepOverStopsAtCurrentDepthAndOwnExitButRunsThroughCallees() {
        var controller = new StepController();
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
        var controller = new StepController();
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
        var controller = new StepController();
        controller.arm(DebugCommand.STEP_INTO, 2);
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.ENTER, 5, "uri", null, null, 0));
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 1, "uri", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void pauseSuspendsAtNextEvent() {
        var controller = new StepController();
        controller.armInitial(false);
        controller.requestPause();
        assertTrue(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(0, 0), null, 0));
    }

    @Test
    void armClearsPendingPause() {
        var controller = new StepController();
        controller.requestPause();
        controller.arm(DebugCommand.RESUME, 2);
        assertFalse(controller.shouldSuspend(DebugEvent.LOCATION, 5, "uri", CurrentLocation.cell(0, 0), null, 0));
    }
}
