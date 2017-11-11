package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.util.Arrays.isEmpty;
import static org.openl.rules.util.Arrays.isNotEmpty;
import static org.openl.rules.util.Arrays.length;

import java.util.ArrayList;
import java.util.HashSet;

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
    }
}
