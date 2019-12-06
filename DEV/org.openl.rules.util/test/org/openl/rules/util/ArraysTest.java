package org.openl.rules.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.util.Arrays.isEmpty;
import static org.openl.rules.util.Arrays.isNotEmpty;
import static org.openl.rules.util.Arrays.length;
import static org.openl.rules.util.Arrays.slice;

import org.junit.Test;

public class ArraysTest {
    @Test
    public void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(new Object[0]));
        assertFalse(isEmpty(new Object[] { 0 }));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(isNotEmpty(null));
        assertFalse(isNotEmpty(new Object[0]));
        assertTrue(isNotEmpty(new Object[] { 0 }));
    }

    @Test
    public void testLength() {
        assertEquals(0, length(null));
        assertEquals(0, length(new Object[0]));
        assertEquals(1, length(new Object[] { 0 }));
    }

    @Test
    public void testSlice() {
        assertNull(slice(null, 0));
        assertArrayEquals(new Object[0], slice(new Object[0], 0));
        assertArrayEquals(new Object[] { 5, 4, 3, 2, 1 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 0));
        assertArrayEquals(new Object[] { 3, 2, 1 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 2));
        assertArrayEquals(new Object[] { 2, 1 }, slice(new Object[] { 5, 4, 3, 2, 1 }, -2));
        assertArrayEquals(new Object[0], slice(new Object[] { 5, 4, 3, 2, 1 }, 5));
    }

    @Test
    public void testSlice2() {
        assertNull(slice(null, 0, 2));
        assertArrayEquals(new Object[0], slice(new Object[0], 0, -2));
        assertArrayEquals(new Object[] { 5, 4, 3, 2 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 0, 4));
        assertArrayEquals(new Object[] { 3, 2 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 2, -1));
        assertArrayEquals(new Object[] { 4, 3 }, slice(new Object[] { 5, 4, 3, 2, 1 }, -4, 3));
        assertArrayEquals(new Object[0], slice(new Object[] { 5, 4, 3, 2, 1 }, -8, -5));
    }
}
