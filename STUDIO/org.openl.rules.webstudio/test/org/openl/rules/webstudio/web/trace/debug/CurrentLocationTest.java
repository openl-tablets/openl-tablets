package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class CurrentLocationTest {

    @Test
    void breakpointRefsIsTheCellReferenceForACell() {
        assertEquals(List.of("R2C3"), CurrentLocation.cell(2, 3).breakpointRefs());
    }

    @Test
    void aSingleFiredRuleIsItsOwnTarget() {
        var location = CurrentLocation.dtRule(List.of("R10"));
        assertEquals("R10", location.ref());
        assertEquals("R10", location.label());
    }

    @Test
    void manyFiredRulesTargetTheAnyRuleKeyWithACompactLabel() {
        var rules = IntStream.rangeClosed(1, 357).mapToObj(i -> "R" + i).toList();
        var location = CurrentLocation.dtRule(rules);
        // Every fired rule remains a matchable breakpoint, but the step targets a single valid key so it
        // stays replayable instead of being an un-targetable comma-joined list.
        assertTrue(location.breakpointRefs().containsAll(List.of(CurrentLocation.RULE_FIRED_REF, "R1", "R357")));
        assertEquals(CurrentLocation.RULE_FIRED_REF, location.ref());
        assertEquals("R1, R2, R3 +354 more", location.label());
    }

    @Test
    void aFewFiredRulesAreListedInFull() {
        var location = CurrentLocation.dtRule(List.of("R10", "R11"));
        assertEquals(List.of(CurrentLocation.RULE_FIRED_REF, "R10", "R11"), location.breakpointRefs());
        assertEquals(CurrentLocation.RULE_FIRED_REF, location.ref());
        assertEquals("R10, R11", location.label());
    }

    @Test
    void breakpointRefsAreEmptyForAnOperation() {
        assertEquals(List.of(), CurrentLocation.operation("ADD").breakpointRefs());
    }
}
