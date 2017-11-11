package org.openl.rules.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.util.Miscs.isEmpty;
import static org.openl.rules.util.Miscs.isInfinite;
import static org.openl.rules.util.Miscs.isNaN;
import static org.openl.rules.util.Miscs.isNotEmpty;

import java.util.ArrayList;

import org.junit.Test;

public class MiscsTest {
    @Test
    public void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(new Object[0]));
        assertFalse(isEmpty(new Object[] { 0 }));

        assertTrue(isEmpty(new byte[0]));
        assertFalse(isEmpty(new byte[] { 0 }));
        assertTrue(isEmpty(new ArrayList()));
        assertFalse(isEmpty(new ArrayList() {
            {
                add(1);
            }
        }));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(isNotEmpty(null));
        assertFalse(isNotEmpty(new Object[0]));
        assertTrue(isNotEmpty(new Object[] { 0 }));

        assertFalse(isNotEmpty(new byte[0]));
        assertTrue(isNotEmpty(new byte[] { 0 }));
        assertFalse(isNotEmpty(new ArrayList()));
        assertTrue(isNotEmpty(new ArrayList() {
            {
                add(1);
            }
        }));
    }

    @Test
    public void testIsNaN() {
        assertNull(isNaN((Double) null));
        assertTrue(isNaN(Double.NaN));
        assertFalse(isNaN(0.0));

        assertNull(isNaN((Float) null));
        assertTrue(isNaN(Float.NaN));
        assertFalse(isNaN(0f));
    }

    @Test
    public void testIsInfinite() {
        assertNull(isInfinite((Double) null));
        assertTrue(isInfinite(Double.NEGATIVE_INFINITY));
        assertTrue(isInfinite(Double.POSITIVE_INFINITY));
        assertFalse(isInfinite(Double.NaN));
        assertFalse(isInfinite(0.0));

        assertNull(isInfinite((Float) null));
        assertTrue(isInfinite(Float.NEGATIVE_INFINITY));
        assertTrue(isInfinite(Float.POSITIVE_INFINITY));
        assertFalse(isInfinite(Float.NaN));
        assertFalse(isInfinite(0f));

        assertNull(isInfinite((Long) null));
        assertTrue(isInfinite(Long.MAX_VALUE));
        assertTrue(isInfinite(Long.MIN_VALUE));
        assertFalse(isInfinite(1L));

        assertNull(isInfinite((Integer) null));
        assertTrue(isInfinite(Integer.MAX_VALUE));
        assertTrue(isInfinite(Integer.MIN_VALUE));
        assertFalse(isInfinite(0));
    }
}
