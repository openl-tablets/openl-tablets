package org.openl.rules.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.util.Arrays.isEmpty;
import static org.openl.rules.util.Arrays.isNotEmpty;

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
    }
}
