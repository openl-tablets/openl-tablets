package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CurrentLocationTest {

    @Test
    void breakpointRefIsTheCellReferenceForACell() {
        assertEquals("R2C3", CurrentLocation.cell(2, 3).breakpointRef());
    }

    @Test
    void breakpointRefIsTheReservedRuleSuffixForAFiredRule() {
        assertEquals(CurrentLocation.RULE_FIRED_REF, CurrentLocation.dtRule("R10").breakpointRef());
        // The displayed ref stays null, so the location view is unchanged.
        assertNull(CurrentLocation.dtRule("R10").ref());
    }

    @Test
    void breakpointRefIsAbsentForAnOperation() {
        assertNull(CurrentLocation.operation("ADD").breakpointRef());
    }
}
