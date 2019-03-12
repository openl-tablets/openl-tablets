package org.openl.rules.indexer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.meta.IntValue;
import org.openl.meta.ShortValue;
import org.openl.rules.TestUtils;

public class ArrayIndexTest {
    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create("./test/rules/ArrayIndexTest.xlsx");
    }

    @Test
    public void testArray1() {// ShortValue
        assertEquals(new ShortValue((short) -10), TestUtils.invoke(instance, "isWork1"));
    }

    @Test
    public void testArray2() {// IntValue
        assertEquals(new IntValue(-10), TestUtils.invoke(instance, "isWork2"));
    }

    @Test
    public void testArray3() {// int
        assertEquals(-10, (int) TestUtils.invoke(instance, "isWork3"));
    }

    @Test
    public void testArray4() {// long
        assertEquals(-10, (long) TestUtils.invoke(instance, "isWork4"));
    }

    @Test
    public void testArray5() {// Long
        assertEquals(-10, (long) TestUtils.invoke(instance, "isWork5"));
    }

    @Test
    public void testArray6() {// Byte
        assertEquals(-10, (byte) TestUtils.invoke(instance, "isWork6"));
    }

    @Test
    public void testArray7() {// byte
        assertEquals(-10, (byte) TestUtils.invoke(instance, "isWork7"));
    }

    @Test
    public void testArray8() {// Integer
        assertEquals(-10, (int) TestUtils.invoke(instance, "isWork8"));
    }
}