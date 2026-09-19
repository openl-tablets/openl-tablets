package org.openl.rules.cmatch.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.helpers.IntRange;

class NumberMatchMatcherTest {

    private final NumberMatchMatcher matcher = new NumberMatchMatcher(Integer.class, IntRange.class);

    @Test
    void anEmptyCellMatchesNothing() {
        assertNull(matcher.fromString(""));
        assertFalse(matcher.match(1, matcher.fromString("")));
    }

    @Test
    void readsASingleNumberAndARange() {
        assertEquals(7, matcher.fromString("7"));
        assertEquals(new IntRange(1, 5), matcher.fromString("1-5"));
    }

    @Test
    void matchesAValueAgainstANumberAndAgainstARange() {
        assertTrue(matcher.match(7, matcher.fromString("7")));
        assertFalse(matcher.match(8, matcher.fromString("7")));

        assertTrue(matcher.match(3, matcher.fromString("1-5")));
        assertFalse(matcher.match(9, matcher.fromString("1-5")));
    }
}
