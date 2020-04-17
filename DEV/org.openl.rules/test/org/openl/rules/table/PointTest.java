package org.openl.rules.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PointTest {

    @Test
    public void testEquals() {
        Point point1 = Point.get(1, 2);
        Point point2 = Point.get(3, 4);
        Point point3 = Point.get(1, 2);

        assertNotEquals(point1, point2);
        assertEquals(point1, point3);
    }

    @Test
    public void testHahCode() {
        Point point1 = Point.get(1, 2);
        Point point2 = Point.get(3, 4);
        Point point3 = Point.get(1, 2);

        assertNotEquals(point1.hashCode(), point2.hashCode());
        assertEquals(point1.hashCode(), point3.hashCode());
    }

}
