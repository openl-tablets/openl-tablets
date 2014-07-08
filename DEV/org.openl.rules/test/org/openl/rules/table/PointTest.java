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

    @Test
    public void testMoveDown() {
        Point test = new Point(1, 3);
        Point moved = test.moveDown(1);
        assertFalse("New instance is returned", test.equals(moved));
        assertEquals(4, moved.getRow());
        assertEquals(1, moved.getColumn());
    }

    @Test
    public void testShiftRight() {
        Point test = new Point(1, 3);
        Point moved = test.moveRight();
        assertFalse("New instance is returned", test.equals(moved));
        assertEquals(2, moved.getColumn());
        assertEquals(3, moved.getRow());
    }

    @Test
    public void testMoveRightAndDown() {
        Point test = new Point(1, 3);
        Point moved = test.moveRightAndDown();
        assertFalse("New instance is returned", test.equals(moved));
        assertEquals(2, moved.getColumn());
        assertEquals(4, moved.getRow());
    }

}
