package org.openl.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class IntArrayIteratorTest {

    @Test
    void testIterator() {
        int[] arr = {1, 2, 3, 5, 8, 13, 21, 34};
        var it = new IntArrayIterator(arr);
        assertTrue(it.isResetable());
        assertEquals(arr.length, it.size());
        for (final int expected : arr) {
            assertTrue(it.hasNext());
            final var actual = it.nextInt();
            assertEquals(expected, actual);
        }
        assertFalse(it.hasNext());
        it.reset();
        for (final int expected : arr) {
            assertTrue(it.hasNext());
            final var actual = it.next();
            assertEquals((Integer) expected, actual);
        }
        assertFalse(it.hasNext());
    }

    @Test
    void anExhaustedIteratorHasNothingToAnswerWith() {
        var it = new IntArrayIterator(new int[]{1});

        assertEquals(1, it.nextInt());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
        assertThrows(NoSuchElementException.class, it::next);
    }
}
