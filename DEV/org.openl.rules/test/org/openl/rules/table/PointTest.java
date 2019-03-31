package org.openl.rules.table;

import static org.junit.Assert.*;

import org.junit.Test;

public class PointTest {

    @Test
    public void testEquals() {
        Point point1 = new Point(1, 2);
        Point point2 = new Point(3, 4);
        Point point3 = new Point(1, 2);

        assertFalse(point1.equals(point2));
        assertTrue(point1.equals(point3));
    }

    @Test
    public void testHahCode() {
        Point point1 = new Point(1, 2);
        Point point2 = new Point(3, 4);
        Point point3 = new Point(1, 2);
        assertTrue(point1.hashCode() != point2.hashCode());
        assertTrue(point1.hashCode() == point3.hashCode());
    }

}
