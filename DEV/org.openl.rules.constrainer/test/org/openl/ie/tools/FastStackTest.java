package org.openl.ie.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FastStackTest {

    @Test
    void aCopyHoldsTheSameElementsAndIsIndependent() {
        var original = new FastStack();
        original.push("first");
        original.push("second");

        var copy = new FastStack(original);
        original.pop();
        original.push("changed");

        assertEquals(2, copy.size());
        assertEquals("second", copy.pop());
        assertEquals("first", copy.pop());
        assertTrue(copy.empty());
        assertEquals("changed", original.peek());
    }
}
