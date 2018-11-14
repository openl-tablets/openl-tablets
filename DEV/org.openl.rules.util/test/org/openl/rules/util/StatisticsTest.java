package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.openl.rules.util.Statistics.max;
import static org.openl.rules.util.Statistics.min;
import static org.openl.rules.util.Statistics.sum;

import org.junit.Test;

public class StatisticsTest {

    @Test
    public void testMax() {
        assertNull(max());
        assertNull(max(null));
        assertNull(max(new Integer[0]));
        assertNull(max(new Integer[] { null }));

        assertEquals(Integer.valueOf(10), max(1, 10, 9));
        assertEquals(Double.valueOf(9.5), max(9.5));
        assertEquals(Integer.valueOf(8), max(8, null, 2));
        assertEquals(Double.valueOf(7.0), max(-10.0, 6.0, 7.0));
    }

    @Test
    public void testMin() {
        assertNull(min());
        assertNull(min(null));
        assertNull(min(new Integer[0]));
        assertNull(min(new Integer[] { null }));

        assertEquals(Integer.valueOf(1), min(1, 10, 9));
        assertEquals(Double.valueOf(9.5), min(9.5));
        assertEquals(Integer.valueOf(2), min(8, null, 2));
        assertEquals(Double.valueOf(-10.0), min(-10.0, 6.0, 7.0));
    }

    @Test
    public void testSum() {
        assertNull(sum((Integer[]) null));
        assertNull(sum(new Integer[0]));
        assertNull(sum(new Integer[] { null }));

        assertEquals(Integer.valueOf(20), sum(1, 10, 9));
        assertEquals(Double.valueOf(9.5), sum(9.5));
        assertEquals(Integer.valueOf(10), sum(8, null, 2));
        assertEquals(Double.valueOf(3.0), sum(-10.0, 6.0, 7.0));

        assertEquals(Integer.valueOf(15), sum((byte) 3, (byte) 4, (byte) 8));
        assertEquals(Integer.valueOf(15), sum((short) 3, (short) 4, (short) 8));
        assertEquals(Integer.valueOf(15), sum(3, 4, 8));
        assertEquals(Long.valueOf(15), sum(3l, 4l, 8l));
        assertEquals(Float.valueOf(15), sum(3f, 4f, 8f));
        assertEquals(Double.valueOf(15), sum(3d, 4d, 8d));
        assertEquals(Double.valueOf(15), sum((byte) 3, (short) 4, 8));
    }
}
