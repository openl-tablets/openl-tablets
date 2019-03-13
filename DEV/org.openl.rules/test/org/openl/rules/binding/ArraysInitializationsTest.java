package org.openl.rules.binding;

import static org.junit.Assert.assertArrayEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class ArraysInitializationsTest {

    private static final String SRC = "test/rules/binding/ArraysInitializationsTest.xls";
    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC);
    }

    @Test
    public void testInitializationJavaStyle() {
        assertArrayEquals(new Integer[] { 1, 2, 3 }, TestUtils.invoke(instance, "array"));
        assertArrayEquals(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } }, TestUtils.invoke(instance, "arrayTwoDims"));
    }

    @Test
    public void testInitializationForLocalVar() {
        assertArrayEquals(new Integer[] { 1, 2 }, TestUtils.invoke(instance, "localVarArrayInit"));
        assertArrayEquals(new Integer[][] { { 11, 12, 13 }, { 21, 22, 23 } },
            TestUtils.invoke(instance, "localVarArrayTwoDimsInit"));
    }

    @Test
    public void testSimpleInitializationForLocalVar() {
        assertArrayEquals(new Integer[] { 1, 2, 3 }, TestUtils.invoke(instance, "localVarSimpleArrayInit"));
        assertArrayEquals(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } },
            TestUtils.invoke(instance, "localVarSimpleArrayTwoDimsInit"));
    }

    @Test
    public void testSimpleInitializationInReturn() {
        assertArrayEquals(new Integer[] { 1, 2, 3 }, TestUtils.invoke(instance, "simpleArrayInitInReturn"));
        assertArrayEquals(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } },
            TestUtils.invoke(instance, "simpleArrayTwoDimsInitInReturn"));
    }
}
