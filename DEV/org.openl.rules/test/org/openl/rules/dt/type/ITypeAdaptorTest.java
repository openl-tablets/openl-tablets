package org.openl.rules.dt.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ITypeAdaptorTest {

    @Test
    void incrementsTheLastCharacter() {
        assertEquals("abd", ITypeAdaptor.incrementString("abc"));
        assertEquals("b", ITypeAdaptor.incrementString("a"));
    }

    @Test
    void carriesOverTheLargestCharacter() {
        assertEquals("ac\u0000", ITypeAdaptor.incrementString("ab￿"));
        assertEquals("b\u0000\u0000", ITypeAdaptor.incrementString("a￿￿"));
    }

    @Test
    void keepsAnEmptyValue() {
        assertEquals("", ITypeAdaptor.incrementString(""));
    }
}
