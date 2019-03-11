package org.openl.rules.operators;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.rules.TestUtils;

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
        assertEquals("passed", instance.testRemInt((int) 22, (int) 11));
        assertEquals("not passed", instance.testRemInt((int) 3, (int) 2));
    }

    @Test
    public void testLong() {
        assertEquals("passed", instance.testRemLong((long) 1000, (long) 500));
        assertEquals("not passed", instance.testRemLong((long) 5, (long) 2));
    }

    @Test
    public void testFloat() {
        assertEquals("passed", instance.testRemFloat((float) 4.44, (float) 2.22));
        assertEquals("not passed", instance.testRemFloat((float) 5.57, (float) 44));
    }

    @Test
    public void testDouble() {
        assertEquals("passed", instance.testRemDouble((double) 4.44, (double) 2.22));
        assertEquals("not passed", instance.testRemDouble((double) 5.57, (double) 44));
    }

    @Test
    public void testByteValue() {
        assertEquals("passed", instance.testRemByteValue(new ByteValue((byte) 4), new ByteValue((byte) 2)));
        assertEquals("not passed", instance.testRemByteValue(new ByteValue((byte) 3), new ByteValue((byte) 5)));
    }

    @Test
    public void testShortValue() {
        assertEquals("passed", instance.testRemShortValue(new ShortValue((short) 100), new ShortValue((short) 20)));
        assertEquals("not passed", instance.testRemShortValue(new ShortValue((short) 103), new ShortValue((short) 2)));
    }

    @Test
    public void testIntValue() {
        assertEquals("passed", instance.testRemIntValue(new IntValue((int) 22), new IntValue((int) 11)));
        assertEquals("not passed", instance.testRemIntValue(new IntValue((int) 3), new IntValue((int) 2)));
    }

    @Test
    public void testLongValue() {
        assertEquals("passed", instance.testRemLongValue(new LongValue((long) 1000), new LongValue((long) 500)));
        assertEquals("not passed", instance.testRemLongValue(new LongValue((long) 5), new LongValue((long) 2)));
    }

    @Test
    public void testFloatValue() {
        assertEquals("passed", instance.testRemFloatValue(new FloatValue((float) 4.44), new FloatValue((float) 2.22)));
        assertEquals("not passed",
            instance.testRemFloatValue(new FloatValue((float) 5.57), new FloatValue((float) 44)));
    }

    @Test
    public void testDoubleValue() {
        assertEquals("passed",
            instance.testRemDoubleValue(new DoubleValue((double) 4.44), new DoubleValue((double) 2.22)));
        assertEquals("not passed",
            instance.testRemDoubleValue(new DoubleValue((double) 5.57), new DoubleValue((double) 44)));
    }

    @Test
    public void testBigIntegerValue() {
        assertEquals("passed", instance.testRemBigIntegerValue(new BigIntegerValue("10"), new BigIntegerValue("5")));
        assertEquals("not passed",
            instance.testRemBigIntegerValue(new BigIntegerValue("17"), new BigIntegerValue("3")));
    }

    @Test
    public void testBigDecimalValue() {
        assertEquals("passed",
            instance.testRemBigDecimalValue(new BigDecimalValue("4.44"), new BigDecimalValue("2.22")));
        assertEquals("not passed",
            instance.testRemBigDecimalValue(new BigDecimalValue("5.57"), new BigDecimalValue("44")));
    }

    public interface RulesInterf {
        String testRemByte(byte v1, byte v2);

        String testRemShort(short v1, short v2);

        String testRemInt(int v1, int v2);

        String testRemLong(long v1, long v2);

        String testRemFloat(float v1, float v2);

        String testRemDouble(double v1, double v2);

        String testRemByteValue(ByteValue v1, ByteValue v2);

        String testRemShortValue(ShortValue v1, ShortValue v2);

        String testRemIntValue(IntValue v1, IntValue v2);

        String testRemLongValue(LongValue v1, LongValue v2);

        String testRemFloatValue(FloatValue v1, FloatValue v2);

        String testRemDoubleValue(DoubleValue v1, DoubleValue v2);

        String testRemBigIntegerValue(BigIntegerValue v1, BigIntegerValue v2);

        String testRemBigDecimalValue(BigDecimalValue v1, BigDecimalValue v2);
    }

}
