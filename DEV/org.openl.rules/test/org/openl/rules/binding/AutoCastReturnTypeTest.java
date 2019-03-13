package org.openl.rules.binding;

import static org.junit.Assert.assertArrayEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class AutoCastReturnTypeTest {

    private static final String SRC = "test/rules/binding/AutoCastReturnTypeMethodsTest.xlsx";
    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC);
    }

    @Test
    public void testInitializationJavaStyle() {
        assertArrayEquals(new String[] { "1", "2", "3", "1", "2", "3" }, TestUtils.invoke(instance, "flattenTest"));
        assertArrayEquals(new Integer[] { 1, 2, -5, 0 }, TestUtils.invoke(instance, "getValuesTest"));
    }
}