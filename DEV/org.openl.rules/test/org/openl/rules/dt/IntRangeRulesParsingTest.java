package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.rules.dt.type.IntRangeAdaptor;

/**
 *
 * @author DLiauchuk
 *
 */
public class IntRangeRulesParsingTest {

    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create("test/rules/helpers/IntRangeTest.xlsx");
    }

    /**
     * Test to check EPBDS-2128 issue.
     */
    @Test
    public void test() {
        assertEquals("rule2", invoke("getLossAssessment", true, 9999));
    }

    /**
     * Test that negative int ranges are supported.
     */
    @Test
    public void testNegativeRange() {
        assertEquals("rule1", invoke("testNegativeRange", true, -205));

        assertEquals("rule2", invoke("testNegativeRange", true, -103));

        assertEquals("rule3", invoke("testNegativeRange", false, -80));

        assertEquals("rule3", invoke("testNegativeRange", false, -100));

        assertEquals("rule4", invoke("testNegativeRange", true, -20));

        assertEquals("rule5", invoke("testNegativeRange", false, -20));
    }

    /**
     * Test that byte value can be processed through IntRange
     */
    @Test
    public void testByteRange() {
        assertEquals("rule2", invoke("testByteRange", true, 110));
    }

    /**
     * Test that short value can be processed through IntRange
     */
    @Test
    public void testShortRange() {
        assertEquals("rule1", invoke("testShortRange", true, 202));
    }

    /**
     * Test that Long.MAX_VALUE won`t get to range. As during current implementation it can`t be covered. See
     * {@link IntRangeAdaptor#getMax(org.openl.rules.helpers.IntRange)}
     */
    @Test
    public void testMaxInt() {
        assertNull(invoke("testMaxInt", true, Long.MAX_VALUE - 1));
    }

    @Test
    public void testMaxInt1() {
        assertEquals("rule1", invoke("testMaxInt1", true, 9223372036854775701l));
    }

    @Test
    public void testtestRange() {
        assertEquals("rule1",
            TestUtils.invoke(instance,
                "ClassifyIncome",
                new Class[] { String.class, short.class },
                new Object[] { "Type 1", (short) -300 }));
    }

    @Test
    public void testtestRange0() {
        assertEquals("rule3",
            TestUtils.invoke(instance,
                "ClassifyIncome",
                new Class[] { String.class, short.class },
                new Object[] { "Type 2", (short) -80 }));
    }

    private Object invoke(String methodName, boolean param1, long param2) {
        return TestUtils
            .invoke(instance, methodName, new Class[] { boolean.class, long.class }, new Object[] { param1, param2 });
    }

}
