package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Test;

/**
 * @author Yury Molchan
 */
class LikePatternTest {

    @Test
    void compilesAValidPattern() {
        assertTrue(LikePattern.compile("[A-D]####").matcher("B1010").matches());
    }

    @Test
    void rejectsAReversedRange() {
        var error = assertThrows(PatternSyntaxException.class, () -> LikePattern.compile("[a-A]"));
        assertEquals("Illegal character range", error.getDescription());
    }

    @Test
    void rejectsAnUnclosedSet() {
        assertThrows(PatternSyntaxException.class, () -> LikePattern.compile("[ab"));
    }
}
