package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class DoubleRangeRulesParsingTest {

    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create("test/rules/helpers/DoubleRangeRulesParsingTest.xlsx");
    }

    /**
     * checks that negative double ranges are being parsed from excel rule.
     */
    @Test
    public void testDoubleNegativeRange() {
        assertEquals("rule1", invoke("testDoubleNegativeRange", "Hello1", -200.5));
        assertEquals("rule2", invoke("testDoubleNegativeRange", "Hello2", -60.5));
        assertEquals("rule3", invoke("testDoubleNegativeRange", "Hello3", -40.67));
    }

    /**
     * Test that long can be processed through DoubleRange
     */
    @Test
    public void testLongRange() {
        Object result = invoke("testLongRange", true, 210);
        assertEquals("rule1", result);
    }

    /**
     * Test that int can be processed through DoubleRange
     */
    @Test
    public void testIntRange() {
        Object result = invoke("testIntegerRange", true, 105);
        assertEquals("rule2", result);
    }

    /**
     * Test that int can be processed through DoubleRange via DecisionTable with one condition(special case)
     */
    @Test
    public void testIntRange1() {
        Object result = invoke("testIntegerRange1", true, 105);
        assertEquals("rule2", result);
    }

    /**
     * Test that byte can be processed through DoubleRange
     */
    @Test
    public void testByteRange() {
        Object result = invoke("testByteRange", true, 99);
        assertEquals("rule2", result);
    }

    /**
     * Test that short can be processed through DoubleRange
     */
    @Test
    public void testShortRange() {
        Object result = invoke("testShortRange", false, -50);
        assertEquals("rule3", result);
    }

    /**
     * Test that float can be processed through DoubleRange
     */
    @Test
    public void testFloatRange() {
        Object result = invoke("testFloatRange", true, 20.56);
        assertEquals("rule1", result);
    }

    private Object invoke(String methodName, boolean param1, double param2) {
        return TestUtils
            .invoke(instance, methodName, new Class[] { boolean.class, double.class }, new Object[] { param1, param2 });
    }

    private Object invoke(String methodName, String param1, double param2) {
        return TestUtils
            .invoke(instance, methodName, new Class[] { String.class, double.class }, new Object[] { param1, param2 });
    }
}
