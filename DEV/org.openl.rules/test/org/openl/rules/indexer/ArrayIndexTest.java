package org.openl.rules.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class ArrayIndexTest {
    private static Object instance;

    @BeforeAll
    static void init() {
        instance = TestUtils.create("./test/rules/ArrayIndexTest.xlsx");
    }

    @Test
    void testArray1() {// ShortValue
        assertEquals(Short.valueOf((short) -10), TestUtils.invoke(instance, "isWork1"));
    }

    @Test
    void testArray2() {// IntValue
        assertEquals(Integer.valueOf(-10), TestUtils.invoke(instance, "isWork2"));
    }

    @Test
    void testArray3() {// int
        assertEquals(-10, (int) TestUtils.invoke(instance, "isWork3"));
    }

    @Test
    void testArray4() {// long
        assertEquals(-10, (long) TestUtils.invoke(instance, "isWork4"));
    }

    @Test
    void testArray5() {// Long
        assertEquals(-10, (long) TestUtils.invoke(instance, "isWork5"));
    }

    @Test
    void testArray6() {// Byte
        assertEquals(-10, (byte) TestUtils.invoke(instance, "isWork6"));
    }

    @Test
    void testArray7() {// byte
        assertEquals(-10, (byte) TestUtils.invoke(instance, "isWork7"));
    }

    @Test
    void testArray8() {// Integer
        assertEquals(-10, (int) TestUtils.invoke(instance, "isWork8"));
    }
}
