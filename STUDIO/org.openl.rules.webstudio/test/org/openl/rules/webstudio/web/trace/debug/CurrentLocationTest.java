package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class CurrentLocationTest {

    @Test
    void breakpointRefsIsTheCellReferenceForACell() {
        assertEquals(List.of("R2C3"), CurrentLocation.cell(2, 3).breakpointRefs());
    }

    @Test
    void breakpointRefsCoverAnyRuleAndEachFiredRule() {
        var location = CurrentLocation.dtRule(List.of("R10", "R11"));
        assertEquals(List.of(CurrentLocation.RULE_FIRED_REF, "R10", "R11"), location.breakpointRefs());
        // The displayed ref stays null; the label joins the fired rule names.
        assertNull(location.ref());
        assertEquals("R10, R11", location.label());
    }

    @Test
    void breakpointRefsAreEmptyForAnOperation() {
        assertEquals(List.of(), CurrentLocation.operation("ADD").breakpointRefs());
    }
}
