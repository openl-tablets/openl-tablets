package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class AIndexedIteratorTest {

    @Test
    void walksTheWholeArray() {
        var it = AIndexedIterator.fromArrayObj(new int[]{1, 2, 3});

        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void anExhaustedIteratorHasNothingToAnswerWith() {
        var it = AIndexedIterator.fromArrayObj(new int[]{1});

        assertEquals(1, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void anAbsentArrayIsAnEmptyIteration() {
        var it = AIndexedIterator.fromArrayObj(null);

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }
}
