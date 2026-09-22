package org.openl.util.formatters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

/** What text an enumeration formatter reads a constant out of, and what text it refuses. */
class EnumFormatterTest {

    /** An enumeration written the way the property enumerations are: a constant with a display name of its own. */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private enum Validate {
        ON("On"),
        OFF("Off");

        private final String displayName;

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final EnumFormatter formatter = new EnumFormatter(Validate.class);

    @Test
    void readsTheConstantTheWholeTextNames() {
        assertEquals(Validate.ON, formatter.parse("ON"));
        assertEquals(Validate.OFF, formatter.parse("OFF"));
    }

    @Test
    void readsAConstantWrittenInAnotherCase() {
        // A cell written by hand, or by a version that wrote the display name, holds the value in the case its
        // author used. The engine reads such a cell, so the screen drawing it must read it too.
        assertEquals(Validate.ON, formatter.parse("on"));
        assertEquals(Validate.ON, formatter.parse("On"));
        assertEquals(Validate.OFF, formatter.parse("oFf"));
    }

    @Test
    void refusesTextNamingNoConstant() {
        // Answering with a constant here would put a value in place of what the author wrote; answering with
        // nothing leaves the text as it stands, which is what a reader has to correct.
        assertNull(formatter.parse("maybe"));
        assertNull(formatter.parse("O"));
        assertNull(formatter.parse("ON OFF"));
    }

    @Test
    void readsNoConstantOutOfNothing() {
        assertNull(formatter.parse(null));
        assertNull(formatter.parse(""));
    }

    @Test
    void writesTheConstantItsOwnNameRatherThanItsDisplayName() {
        assertEquals("ON", formatter.format(Validate.ON));
        // A value of another kind is not this enumeration's to write.
        assertNull(formatter.format("ON"));
        assertNull(formatter.format(null));
    }
}
