package org.openl.rules.dt.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class OptimizedDTRangeConditionsTest {

    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create("test/rules/dt/algorithm/OptimizedDTRangeConditions.xls");
    }

    @Test
    public void testIntRangeClosed() {
        assertNull(TestUtils.invoke(instance, "intRangeClosed", 0));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeClosed", 1));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeClosed", 10));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeClosed", 12));
        assertNull(TestUtils.invoke(instance, "intRangeClosed", 14));
        assertEquals("rule2", TestUtils.invoke(instance, "intRangeClosed", 16));
        assertEquals("rule2", TestUtils.invoke(instance, "intRangeClosed", 18));
        assertNull(TestUtils.invoke(instance, "intRangeClosed", 19));
        assertEquals("rule3", TestUtils.invoke(instance, "intRangeClosed", 21));
        assertEquals("rule3", TestUtils.invoke(instance, "intRangeClosed", 26));
        assertNull(TestUtils.invoke(instance, "intRangeClosed", 27));
    }

    @Test
    public void testIntRangeLeftOpened() {
        assertNull(TestUtils.invoke(instance, "intRangeLeftOpened", 0));
        assertNull(TestUtils.invoke(instance, "intRangeLeftOpened", 1));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeLeftOpened", 2));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeLeftOpened", 10));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeLeftOpened", 12));
        assertNull(TestUtils.invoke(instance, "intRangeLeftOpened", 14));
        assertNull(TestUtils.invoke(instance, "intRangeLeftOpened", 16));
        assertEquals("rule2", TestUtils.invoke(instance, "intRangeLeftOpened", 17));
        assertEquals("rule2", TestUtils.invoke(instance, "intRangeLeftOpened", 18));
        assertNull(TestUtils.invoke(instance, "intRangeLeftOpened", 19));
        assertNull(TestUtils.invoke(instance, "intRangeLeftOpened", 21));
        assertEquals("rule3", TestUtils.invoke(instance, "intRangeLeftOpened", 22));
        assertEquals("rule3", TestUtils.invoke(instance, "intRangeLeftOpened", 26));
        assertNull(TestUtils.invoke(instance, "intRangeLeftOpened", 27));
    }

    @Test
    public void testIntRangeRightOpened() {
        assertNull(TestUtils.invoke(instance, "intRangeRightOpened", 0));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeRightOpened", 1));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeRightOpened", 10));
        assertNull(TestUtils.invoke(instance, "intRangeRightOpened", 12));
        assertNull(TestUtils.invoke(instance, "intRangeRightOpened", 14));
        assertEquals("rule2", TestUtils.invoke(instance, "intRangeRightOpened", 16));
        assertEquals("rule2", TestUtils.invoke(instance, "intRangeRightOpened", 17));
        assertNull(TestUtils.invoke(instance, "intRangeRightOpened", 18));
        assertNull(TestUtils.invoke(instance, "intRangeRightOpened", 19));
        assertEquals("rule3", TestUtils.invoke(instance, "intRangeRightOpened", 21));
        assertEquals("rule3", TestUtils.invoke(instance, "intRangeRightOpened", 25));
        assertNull(TestUtils.invoke(instance, "intRangeRightOpened", 26));
        assertNull(TestUtils.invoke(instance, "intRangeRightOpened", 27));
    }

    @Test
    public void testIntRangeOpened() {
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 0));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 1));
        assertEquals("rule1", TestUtils.invoke(instance, "intRangeOpened", 10));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 12));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 14));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 16));
        assertEquals("rule2", TestUtils.invoke(instance, "intRangeOpened", 17));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 18));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 19));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 21));
        assertEquals("rule3", TestUtils.invoke(instance, "intRangeOpened", 25));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 26));
        assertNull(TestUtils.invoke(instance, "intRangeOpened", 27));
    }

    @Test
    public void testDoubleRangeClosed() {
        assertNull(TestUtils.invoke(instance, "doubleRangeClosed", 0.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeClosed", 1.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeClosed", 10.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeClosed", 15.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeClosed", 15.1));
        assertEquals("rule2", TestUtils.invoke(instance, "doubleRangeClosed", 15.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeClosed", 15.5555));
        assertNull(TestUtils.invoke(instance, "doubleRangeClosed", 16.5555));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeClosed", 16.6));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeClosed", 17.1));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeClosed", 17.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeClosed", 17.5001));
    }

    @Test
    public void testDoubleRangeLeftOpened() {
        assertNull(TestUtils.invoke(instance, "doubleRangeLeftOpened", 0.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeLeftOpened", 1.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeLeftOpened", 10.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeLeftOpened", 15.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeLeftOpened", 15.1));
        assertEquals("rule2", TestUtils.invoke(instance, "doubleRangeLeftOpened", 15.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeLeftOpened", 15.5555));
        assertNull(TestUtils.invoke(instance, "doubleRangeLeftOpened", 16.5555));
        assertNull(TestUtils.invoke(instance, "doubleRangeLeftOpened", 16.6));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeLeftOpened", 17.1));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeLeftOpened", 17.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeLeftOpened", 17.5001));
    }

    @Test
    public void testDoubleRangeRightOpened() {
        assertNull(TestUtils.invoke(instance, "doubleRangeRightOpened", 0.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeRightOpened", 1.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeRightOpened", 10.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeRightOpened", 15.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeRightOpened", 15.1));
        assertEquals("rule2", TestUtils.invoke(instance, "doubleRangeRightOpened", 15.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeRightOpened", 15.5555));
        assertNull(TestUtils.invoke(instance, "doubleRangeRightOpened", 16.5555));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeRightOpened", 16.6));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeRightOpened", 17.1));
        assertNull(TestUtils.invoke(instance, "doubleRangeRightOpened", 17.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeRightOpened", 17.5001));
    }

    @Test
    public void testDoubleRangeOpened() {
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 0.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 1.0));
        assertEquals("rule1", TestUtils.invoke(instance, "doubleRangeOpened", 10.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 15.0));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 15.1));
        assertEquals("rule2", TestUtils.invoke(instance, "doubleRangeOpened", 15.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 15.5555));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 16.5555));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 16.6));
        assertEquals("rule3", TestUtils.invoke(instance, "doubleRangeOpened", 17.1));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 17.5));
        assertNull(TestUtils.invoke(instance, "doubleRangeOpened", 17.5001));
    }
}
