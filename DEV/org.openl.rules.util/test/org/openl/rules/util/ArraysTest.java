package org.openl.rules.util;

import static org.junit.Assert.*;
import static org.openl.rules.util.Arrays.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

public class ArraysTest {
    @Test
    public void testIsEmpty() {
        assertTrue(isEmpty((Object[]) null));
        assertTrue(isEmpty(new Object[0]));
        assertFalse(isEmpty(new Object[] { 0 }));

        assertTrue(isEmpty((byte[]) null));
        assertTrue(isEmpty(new byte[0]));
        assertFalse(isEmpty(new byte[] { 0 }));

        assertTrue(isEmpty((char[]) null));
        assertTrue(isEmpty(new char[0]));
        assertFalse(isEmpty(new char[] { 0 }));

        assertTrue(isEmpty((short[]) null));
        assertTrue(isEmpty(new short[0]));
        assertFalse(isEmpty(new short[] { 0 }));

        assertTrue(isEmpty((int[]) null));
        assertTrue(isEmpty(new int[0]));
        assertFalse(isEmpty(new int[] { 0 }));

        assertTrue(isEmpty((long[]) null));
        assertTrue(isEmpty(new long[0]));
        assertFalse(isEmpty(new long[] { 0 }));

        assertTrue(isEmpty((float[]) null));
        assertTrue(isEmpty(new float[0]));
        assertFalse(isEmpty(new float[] { 0 }));

        assertTrue(isEmpty((double[]) null));
        assertTrue(isEmpty(new double[0]));
        assertFalse(isEmpty(new double[] { 0 }));

        assertTrue(isEmpty((boolean[]) null));
        assertTrue(isEmpty(new boolean[0]));
        assertFalse(isEmpty(new boolean[] { false }));

        assertTrue(isEmpty((ArrayList<?>) null));
        assertTrue(isEmpty(new ArrayList()));
        assertFalse(isEmpty(new ArrayList() {
            {
                add(1);
            }
        }));

    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(isNotEmpty((Object[]) null));
        assertFalse(isNotEmpty(new Object[0]));
        assertTrue(isNotEmpty(new Object[] { 0 }));

        assertFalse(isNotEmpty((byte[]) null));
        assertFalse(isNotEmpty(new byte[0]));
        assertTrue(isNotEmpty(new byte[] { 0 }));

        assertFalse(isNotEmpty((char[]) null));
        assertFalse(isNotEmpty(new char[0]));
        assertTrue(isNotEmpty(new char[] { 0 }));

        assertFalse(isNotEmpty((short[]) null));
        assertFalse(isNotEmpty(new short[0]));
        assertTrue(isNotEmpty(new short[] { 0 }));

        assertFalse(isNotEmpty((int[]) null));
        assertFalse(isNotEmpty(new int[0]));
        assertTrue(isNotEmpty(new int[] { 0 }));

        assertFalse(isNotEmpty((long[]) null));
        assertFalse(isNotEmpty(new long[0]));
        assertTrue(isNotEmpty(new long[] { 0 }));

        assertFalse(isNotEmpty((float[]) null));
        assertFalse(isNotEmpty(new float[0]));
        assertTrue(isNotEmpty(new float[] { 0 }));

        assertFalse(isNotEmpty((double[]) null));
        assertFalse(isNotEmpty(new double[0]));
        assertTrue(isNotEmpty(new double[] { 0 }));

        assertFalse(isNotEmpty((boolean[]) null));
        assertFalse(isNotEmpty(new boolean[0]));
        assertTrue(isNotEmpty(new boolean[] { false }));

        assertFalse(isNotEmpty((HashSet<?>) null));
        assertFalse(isNotEmpty(new HashSet()));
        assertTrue(isNotEmpty(new HashSet() {
            {
                add(false);
            }
        }));
    }

    @Test
    public void testLength() {
        assertEquals(0, length((Object[]) null));
        assertEquals(0, length(new Object[0]));
        assertEquals(1, length(new Object[] { 0 }));

        assertEquals(0, length((byte[]) null));
        assertEquals(0, length(new byte[0]));
        assertEquals(1, length(new byte[] { 0 }));

        assertEquals(0, length((char[]) null));
        assertEquals(0, length(new char[0]));
        assertEquals(1, length(new char[] { 0 }));

        assertEquals(0, length((short[]) null));
        assertEquals(0, length(new short[0]));
        assertEquals(1, length(new short[] { 0 }));

        assertEquals(0, length((int[]) null));
        assertEquals(0, length(new int[0]));
        assertEquals(1, length(new int[] { 0 }));

        assertEquals(0, length((long[]) null));
        assertEquals(0, length(new long[0]));
        assertEquals(1, length(new long[] { 0 }));

        assertEquals(0, length((float[]) null));
        assertEquals(0, length(new float[0]));
        assertEquals(1, length(new float[] { 0 }));

        assertEquals(0, length((double[]) null));
        assertEquals(0, length(new double[0]));
        assertEquals(1, length(new double[] { 0 }));

        assertEquals(0, length((boolean[]) null));
        assertEquals(0, length(new boolean[0]));
        assertEquals(1, length(new boolean[] { false }));

        assertEquals(0, length((HashSet<?>) null));
        assertEquals(0, length(new HashSet()));
        assertEquals(1, length(new HashSet() {
            {
                add(false);
            }
        }));

        assertEquals(0, length((Map<?, ?>) null));
        assertEquals(0, length(new HashMap<>()));
        assertEquals(1, length(new HashMap() {
            {
                put(false, true);
            }
        }));
    }

    @Test
    public void testSlice() {
        assertNull(slice((Object[]) null, 0));
        assertArrayEquals(new Object[0], slice(new Object[0], 0));
        assertArrayEquals(new Object[] { 5, 4, 3, 2, 1 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 0));
        assertArrayEquals(new Object[] { 3, 2, 1 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 2));
        assertArrayEquals(new Object[] { 2, 1 }, slice(new Object[] { 5, 4, 3, 2, 1 }, -2));
        assertArrayEquals(new Object[0], slice(new Object[] { 5, 4, 3, 2, 1 }, 5));

        assertNull(slice((byte[]) null, 0));
        assertArrayEquals(new byte[0], slice(new byte[0], 0));
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, slice(new byte[] { 5, 4, 3, 2, 1 }, 0));
        assertArrayEquals(new byte[] { 3, 2, 1 }, slice(new byte[] { 5, 4, 3, 2, 1 }, 2));
        assertArrayEquals(new byte[] { 2, 1 }, slice(new byte[] { 5, 4, 3, 2, 1 }, -2));
        assertArrayEquals(new byte[0], slice(new byte[] { 5, 4, 3, 2, 1 }, 5));

        assertNull(slice((char[]) null, 0));
        assertArrayEquals(new char[0], slice(new char[0], 0));
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, slice(new char[] { 5, 4, 3, 2, 1 }, 0));
        assertArrayEquals(new char[] { 3, 2, 1 }, slice(new char[] { 5, 4, 3, 2, 1 }, 2));
        assertArrayEquals(new char[] { 2, 1 }, slice(new char[] { 5, 4, 3, 2, 1 }, -2));
        assertArrayEquals(new char[0], slice(new char[] { 5, 4, 3, 2, 1 }, 5));

        assertNull(slice((short[]) null, 0));
        assertArrayEquals(new short[0], slice(new short[0], 0));
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, slice(new short[] { 5, 4, 3, 2, 1 }, 0));
        assertArrayEquals(new short[] { 3, 2, 1 }, slice(new short[] { 5, 4, 3, 2, 1 }, 2));
        assertArrayEquals(new short[] { 2, 1 }, slice(new short[] { 5, 4, 3, 2, 1 }, -2));
        assertArrayEquals(new short[0], slice(new short[] { 5, 4, 3, 2, 1 }, 5));

        assertNull(slice((int[]) null, 0));
        assertArrayEquals(new int[0], slice(new int[0], 0));
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, slice(new int[] { 5, 4, 3, 2, 1 }, 0));
        assertArrayEquals(new int[] { 3, 2, 1 }, slice(new int[] { 5, 4, 3, 2, 1 }, 2));
        assertArrayEquals(new int[] { 2, 1 }, slice(new int[] { 5, 4, 3, 2, 1 }, -2));
        assertArrayEquals(new int[0], slice(new int[] { 5, 4, 3, 2, 1 }, 5));

        assertNull(slice((long[]) null, 0));
        assertArrayEquals(new long[0], slice(new long[0], 0));
        assertArrayEquals(new long[] { 5, 4, 3, 2, 1 }, slice(new long[] { 5, 4, 3, 2, 1 }, 0));
        assertArrayEquals(new long[] { 3, 2, 1 }, slice(new long[] { 5, 4, 3, 2, 1 }, 2));
        assertArrayEquals(new long[] { 2, 1 }, slice(new long[] { 5, 4, 3, 2, 1 }, -2));
        assertArrayEquals(new long[0], slice(new long[] { 5, 4, 3, 2, 1 }, 5));

        assertNull(slice((float[]) null, 0));
        assertArrayEquals(new float[0], slice(new float[0], 0), 0.0001f);
        assertArrayEquals(new float[] { 5, 4, 3, 2, 1 }, slice(new float[] { 5, 4, 3, 2, 1 }, 0), 0.0001f);
        assertArrayEquals(new float[] { 3, 2, 1 }, slice(new float[] { 5, 4, 3, 2, 1 }, 2), 0.0001f);
        assertArrayEquals(new float[] { 2, 1 }, slice(new float[] { 5, 4, 3, 2, 1 }, -2), 0.0001f);
        assertArrayEquals(new float[0], slice(new float[] { 5, 4, 3, 2, 1 }, 5), 0.0001f);

        assertNull(slice((double[]) null, 0));
        assertArrayEquals(new double[0], slice(new double[0], 0), 0.0001d);
        assertArrayEquals(new double[] { 5, 4, 3, 2, 1 }, slice(new double[] { 5, 4, 3, 2, 1 }, 0), 0.0001d);
        assertArrayEquals(new double[] { 3, 2, 1 }, slice(new double[] { 5, 4, 3, 2, 1 }, 2), 0.0001d);
        assertArrayEquals(new double[] { 2, 1 }, slice(new double[] { 5, 4, 3, 2, 1 }, -2), 0.0001d);
        assertArrayEquals(new double[0], slice(new double[] { 5, 4, 3, 2, 1 }, 5), 0.0001d);

        assertNull(slice((boolean[]) null, 0));
        assertArrayEquals(new boolean[0], slice(new boolean[0], 0));
        assertArrayEquals(new boolean[] { false, false, true, true, false },
            slice(new boolean[] { false, false, true, true, false }, 0));
    }

    @Test
    public void testSlice2() {
        assertNull(slice((Object[]) null, 0, 2));
        assertArrayEquals(new Object[0], slice(new Object[0], 0, -2));
        assertArrayEquals(new Object[] { 5, 4, 3, 2 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 0, 4));
        assertArrayEquals(new Object[] { 3, 2 }, slice(new Object[] { 5, 4, 3, 2, 1 }, 2, -1));
        assertArrayEquals(new Object[] { 4, 3 }, slice(new Object[] { 5, 4, 3, 2, 1 }, -4, 3));
        assertArrayEquals(new Object[0], slice(new Object[] { 5, 4, 3, 2, 1 }, -8, -5));

        assertNull(slice((byte[]) null, 0, 2));
        assertArrayEquals(new byte[0], slice(new byte[0], 0, -2));
        assertArrayEquals(new byte[] { 5, 4, 3, 2 }, slice(new byte[] { 5, 4, 3, 2, 1 }, 0, 4));
        assertArrayEquals(new byte[] { 3, 2 }, slice(new byte[] { 5, 4, 3, 2, 1 }, 2, -1));
        assertArrayEquals(new byte[] { 4, 3 }, slice(new byte[] { 5, 4, 3, 2, 1 }, -4, 3));
        assertArrayEquals(new byte[0], slice(new byte[] { 5, 4, 3, 2, 1 }, -8, -5));

        assertNull(slice((char[]) null, 0, 2));
        assertArrayEquals(new char[0], slice(new char[0], 0, -2));
        assertArrayEquals(new char[] { 5, 4, 3, 2 }, slice(new char[] { 5, 4, 3, 2, 1 }, 0, 4));
        assertArrayEquals(new char[] { 3, 2 }, slice(new char[] { 5, 4, 3, 2, 1 }, 2, -1));
        assertArrayEquals(new char[] { 4, 3 }, slice(new char[] { 5, 4, 3, 2, 1 }, -4, 3));
        assertArrayEquals(new char[0], slice(new char[] { 5, 4, 3, 2, 1 }, -8, -5));

        assertNull(slice((short[]) null, 0, 2));
        assertArrayEquals(new short[0], slice(new short[0], 0, -2));
        assertArrayEquals(new short[] { 5, 4, 3, 2 }, slice(new short[] { 5, 4, 3, 2, 1 }, 0, 4));
        assertArrayEquals(new short[] { 3, 2 }, slice(new short[] { 5, 4, 3, 2, 1 }, 2, -1));
        assertArrayEquals(new short[] { 4, 3 }, slice(new short[] { 5, 4, 3, 2, 1 }, -4, 3));
        assertArrayEquals(new short[0], slice(new short[] { 5, 4, 3, 2, 1 }, -8, -5));

        assertNull(slice((int[]) null, 0, 2));
        assertArrayEquals(new int[0], slice(new int[0], 0, -2));
        assertArrayEquals(new int[] { 5, 4, 3, 2 }, slice(new int[] { 5, 4, 3, 2, 1 }, 0, 4));
        assertArrayEquals(new int[] { 3, 2 }, slice(new int[] { 5, 4, 3, 2, 1 }, 2, -1));
        assertArrayEquals(new int[] { 4, 3 }, slice(new int[] { 5, 4, 3, 2, 1 }, -4, 3));
        assertArrayEquals(new int[0], slice(new int[] { 5, 4, 3, 2, 1 }, -8, -5));

        assertNull(slice((long[]) null, 0, 2));
        assertArrayEquals(new long[0], slice(new long[0], 0, -2));
        assertArrayEquals(new long[] { 5, 4, 3, 2 }, slice(new long[] { 5, 4, 3, 2, 1 }, 0, 4));
        assertArrayEquals(new long[] { 3, 2 }, slice(new long[] { 5, 4, 3, 2, 1 }, 2, -1));
        assertArrayEquals(new long[] { 4, 3 }, slice(new long[] { 5, 4, 3, 2, 1 }, -4, 3));
        assertArrayEquals(new long[0], slice(new long[] { 5, 4, 3, 2, 1 }, -8, -5));

        assertNull(slice((float[]) null, 0, 2));
        assertArrayEquals(new float[0], slice(new float[0], 0, -2), 0.0001f);
        assertArrayEquals(new float[] { 5, 4, 3, 2 }, slice(new float[] { 5, 4, 3, 2, 1 }, 0, 4), 0.0001f);
        assertArrayEquals(new float[] { 3, 2 }, slice(new float[] { 5, 4, 3, 2, 1 }, 2, -1), 0.0001f);
        assertArrayEquals(new float[] { 4, 3 }, slice(new float[] { 5, 4, 3, 2, 1 }, -4, 3), 0.0001f);
        assertArrayEquals(new float[0], slice(new float[] { 5, 4, 3, 2, 1 }, -8, -5), 0.0001f);

        assertNull(slice((double[]) null, 0, 2));
        assertArrayEquals(new double[0], slice(new double[0], 0, -2), 0.0001d);
        assertArrayEquals(new double[] { 5, 4, 3, 2 }, slice(new double[] { 5, 4, 3, 2, 1 }, 0, 4), 0.0001d);
        assertArrayEquals(new double[] { 3, 2 }, slice(new double[] { 5, 4, 3, 2, 1 }, 2, -1), 0.0001d);
        assertArrayEquals(new double[] { 4, 3 }, slice(new double[] { 5, 4, 3, 2, 1 }, -4, 3), 0.0001d);
        assertArrayEquals(new double[0], slice(new double[] { 5, 4, 3, 2, 1 }, -8, -5), 0.0001d);

        assertNull(slice((boolean[]) null, 0, 2));
        assertArrayEquals(new boolean[0], slice(new boolean[0], 0, -2));
        assertArrayEquals(new boolean[] { true, true },
            slice(new boolean[] { false, false, true, true, false }, 2, -1));
    }
}
