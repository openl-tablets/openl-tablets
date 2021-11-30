package org.openl.rules.operators;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Test remainder operator ('%') in rules
 *
 * @author DLiauchuk
 *
 */
public class RemOperatorTest {

    private static final String SRC = "test/rules/operators/RemOperatorTest.xlsx";

    private static RulesInterf instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC, RulesInterf.class);
    }

    @Test
    public void testByte() {
        assertEquals("passed", instance.testRemByte((byte) 4, (byte) 2));
        assertEquals("not passed", instance.testRemByte((byte) 5, (byte) 2));
    }

    @Test
    public void testShort() {
        assertEquals("passed", instance.testRemShort((short) 100, (short) 20));
        assertEquals("not passed", instance.testRemShort((short) 103, (short) 2));
    }

    @Test
    public void testInt() {
        assertEquals("passed", instance.testRemInt(22, 11));
        assertEquals("not passed", instance.testRemInt(3, 2));
    }

    @Test
    public void testLong() {
        assertEquals("passed", instance.testRemLong(1000, 500));
        assertEquals("not passed", instance.testRemLong(5, 2));
    }

    @Test
    public void testFloat() {
        assertEquals("passed", instance.testRemFloat((float) 4.44, (float) 2.22));
        assertEquals("not passed", instance.testRemFloat((float) 5.57, 44));
    }

    @Test
    public void testDouble() {
        assertEquals("passed", instance.testRemDouble(4.44, 2.22));
        assertEquals("not passed", instance.testRemDouble(5.57, 44));
    }

    @Test
    public void testByteValue() {
        assertEquals("passed", instance.testRemByteValue((byte) 4, (byte) 2));
        assertEquals("not passed", instance.testRemByteValue((byte) 3, (byte) 5));
    }

    @Test
    public void testShortValue() {
        assertEquals("passed", instance.testRemShortValue((short) 100, (short) 20));
        assertEquals("not passed", instance.testRemShortValue((short) 103, (short) 2));
    }

    @Test
    public void testIntValue() {
        assertEquals("passed", instance.testRemIntValue(22, 11));
        assertEquals("not passed", instance.testRemIntValue(3, 2));
    }

    @Test
    public void testLongValue() {
        assertEquals("passed", instance.testRemLongValue(1000L, 500L));
        assertEquals("not passed", instance.testRemLongValue(5L, 2L));
    }

    @Test
    public void testFloatValue() {
        assertEquals("passed", instance.testRemFloatValue(4.44F, 2.22F));
        assertEquals("not passed", instance.testRemFloatValue(5.57F, 44F));
    }

    @Test
    public void testDoubleValue() {
        assertEquals("passed", instance.testRemDoubleValue(4.44, 2.22));
        assertEquals("not passed", instance.testRemDoubleValue(5.57, 44.0));
    }

    @Test
    public void testBigIntegerValue() {
        assertEquals("passed", instance.testRemBigIntegerValue(new BigInteger("10"), new BigInteger("5")));
        assertEquals("not passed",
            instance.testRemBigIntegerValue(new BigInteger("17"), new BigInteger("3")));
    }

    @Test
    public void testBigDecimalValue() {
        assertEquals("passed",
            instance.testRemBigDecimalValue(new BigDecimal("4.44"), new BigDecimal("2.22")));
        assertEquals("not passed",
            instance.testRemBigDecimalValue(new BigDecimal("5.57"), new BigDecimal("44")));
    }

    public interface RulesInterf {
        String testRemByte(byte v1, byte v2);

        String testRemShort(short v1, short v2);

        String testRemInt(int v1, int v2);

        String testRemLong(long v1, long v2);

        String testRemFloat(float v1, float v2);

        String testRemDouble(double v1, double v2);

        String testRemByteValue(Byte v1, Byte v2);

        String testRemShortValue(Short v1, Short v2);

        String testRemIntValue(Integer v1, Integer v2);

        String testRemLongValue(Long v1, Long v2);

        String testRemFloatValue(Float v1, Float v2);

        String testRemDoubleValue(Double v1, Double v2);

        String testRemBigIntegerValue(BigInteger v1, BigInteger v2);

        String testRemBigDecimalValue(BigDecimal v1, BigDecimal v2);
    }

}
