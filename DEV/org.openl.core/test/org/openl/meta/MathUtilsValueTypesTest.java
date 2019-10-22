package org.openl.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.openl.util.math.MathUtils;

public class MathUtilsValueTypesTest {

    @Test
    public void testMedianByteValue() {
        ByteValue[] values = new ByteValue[] { new ByteValue((byte) 4),
                new ByteValue((byte) 4),
                new ByteValue((byte) 1),
                new ByteValue((byte) 7),
                new ByteValue((byte) 2) };
        assertTrue(ByteValue.median(values) instanceof DoubleValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new ByteValue[] { new ByteValue((byte) 4),
                new ByteValue((byte) 5),
                new ByteValue((byte) 1),
                new ByteValue((byte) 7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((ByteValue[]) null));
        assertEquals(null, MathUtils.median(new ByteValue[0]));

        values = new ByteValue[] { null,
                new ByteValue((byte) 4),
                new ByteValue((byte) 4),
                null,
                new ByteValue((byte) 1),
                new ByteValue((byte) 7),
                null,
                new ByteValue((byte) 2) };
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new ByteValue[] { null,
                new ByteValue((byte) 4),
                new ByteValue((byte) 5),
                null,
                new ByteValue((byte) 1),
                null,
                new ByteValue((byte) 7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianShortValue() {
        ShortValue[] values = new ShortValue[] { new ShortValue((short) 4),
                new ShortValue((short) 4),
                new ShortValue((short) 1),
                new ShortValue((short) 7),
                new ShortValue((short) 2) };
        assertTrue(ShortValue.median(values) instanceof DoubleValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new ShortValue[] { new ShortValue((short) 4),
                new ShortValue((short) 5),
                new ShortValue((short) 1),
                new ShortValue((short) 7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((ShortValue[]) null));
        assertEquals(null, MathUtils.median(new ShortValue[0]));

        values = new ShortValue[] { null,
                new ShortValue((short) 4),
                new ShortValue((short) 4),
                null,
                new ShortValue((short) 1),
                new ShortValue((short) 7),
                null,
                new ShortValue((short) 2) };
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new ShortValue[] { null,
                new ShortValue((short) 4),
                new ShortValue((short) 5),
                null,
                new ShortValue((short) 1),
                null,
                new ShortValue((short) 7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianIntValue() {
        IntValue[] values = new IntValue[] { new IntValue(
            4), new IntValue(4), new IntValue(1), new IntValue(7), new IntValue(2) };
        assertTrue(IntValue.median(values) instanceof DoubleValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new IntValue[] { new IntValue(4), new IntValue(5), new IntValue(1), new IntValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((IntValue[]) null));
        assertEquals(null, MathUtils.median(new IntValue[0]));

        values = new IntValue[] { null,
                new IntValue(4),
                new IntValue(4),
                null,
                new IntValue(1),
                new IntValue(7),
                null,
                new IntValue(2) };
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new IntValue[] { null,
                new IntValue(4),
                new IntValue(5),
                null,
                new IntValue(1),
                null,
                new IntValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianLongValue() {
        LongValue[] values = new LongValue[] { new LongValue(
            4), new LongValue(4), new LongValue(1), new LongValue(7), new LongValue(2) };
        assertTrue(LongValue.median(values) instanceof DoubleValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new LongValue[] { new LongValue(4), new LongValue(5), new LongValue(1), new LongValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((LongValue[]) null));
        assertEquals(null, MathUtils.median(new LongValue[0]));

        values = new LongValue[] { null,
                new LongValue(4),
                new LongValue(4),
                null,
                new LongValue(1),
                new LongValue(7),
                null,
                new LongValue(2) };
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new LongValue[] { null,
                new LongValue(4),
                new LongValue(5),
                null,
                new LongValue(1),
                null,
                new LongValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianFloatValue() {
        FloatValue[] values = new FloatValue[] { new FloatValue(
            4), new FloatValue(4), new FloatValue(1), new FloatValue(7), new FloatValue(2) };
        assertTrue(FloatValue.median(values) instanceof FloatValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new FloatValue[] { new FloatValue(4), new FloatValue(5), new FloatValue(1), new FloatValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((FloatValue[]) null));
        assertEquals(null, MathUtils.median(new FloatValue[0]));

        values = new FloatValue[] { null,
                new FloatValue(4),
                new FloatValue(4),
                null,
                new FloatValue(1),
                new FloatValue(7),
                null,
                new FloatValue(2) };
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new FloatValue[] { null,
                new FloatValue(4),
                new FloatValue(5),
                null,
                new FloatValue(1),
                null,
                new FloatValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianDoubleValue() {
        DoubleValue[] values = new DoubleValue[] { new DoubleValue(
            4), new DoubleValue(4), new DoubleValue(1), new DoubleValue(7), new DoubleValue(2) };
        assertTrue(DoubleValue.median(values) instanceof DoubleValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new DoubleValue[] { new DoubleValue(4), new DoubleValue(5), new DoubleValue(1), new DoubleValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((DoubleValue[]) null));
        assertEquals(null, MathUtils.median(new DoubleValue[0]));

        values = new DoubleValue[] { null,
                new DoubleValue(4),
                new DoubleValue(4),
                null,
                new DoubleValue(1),
                new DoubleValue(7),
                null,
                new DoubleValue(2) };
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new DoubleValue[] { null,
                new DoubleValue(4),
                new DoubleValue(5),
                null,
                new DoubleValue(1),
                null,
                new DoubleValue(7) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianBigIntegerValue() {
        BigIntegerValue[] values = new BigIntegerValue[] { new BigIntegerValue(BigInteger.valueOf(4)),
                new BigIntegerValue(BigInteger.valueOf(4)),
                new BigIntegerValue(BigInteger.valueOf(1)),
                new BigIntegerValue(BigInteger.valueOf(7)),
                new BigIntegerValue(BigInteger.valueOf(2)) };
        assertTrue(BigIntegerValue.median(values) instanceof BigDecimalValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new BigIntegerValue[] { new BigIntegerValue(BigInteger.valueOf(4)),
                new BigIntegerValue(BigInteger.valueOf(5)),
                new BigIntegerValue(BigInteger.valueOf(1)),
                new BigIntegerValue(BigInteger.valueOf(7)) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((BigIntegerValue[]) null));
        assertEquals(null, MathUtils.median(new BigIntegerValue[0]));

        values = new BigIntegerValue[] { null,
                new BigIntegerValue(BigInteger.valueOf(4)),
                new BigIntegerValue(BigInteger.valueOf(4)),
                null,
                new BigIntegerValue(BigInteger.valueOf(1)),
                new BigIntegerValue(BigInteger.valueOf(7)),
                null,
                new BigIntegerValue(BigInteger.valueOf(2)) };
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new BigIntegerValue[] { null,
                new BigIntegerValue(BigInteger.valueOf(4)),
                new BigIntegerValue(BigInteger.valueOf(5)),
                null,
                new BigIntegerValue(BigInteger.valueOf(1)),
                null,
                new BigIntegerValue(BigInteger.valueOf(7)) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianBigDecimalValue() {
        BigDecimalValue[] values = new BigDecimalValue[] { new BigDecimalValue(BigDecimal.valueOf(4)),
                new BigDecimalValue(BigDecimal.valueOf(4)),
                new BigDecimalValue(BigDecimal.valueOf(1)),
                new BigDecimalValue(BigDecimal.valueOf(7)),
                new BigDecimalValue(BigDecimal.valueOf(2)) };
        assertTrue(BigDecimalValue.median(values) instanceof BigDecimalValue);
        assertEquals(4.0d, MathUtils.median(values), 0.1);
        values = new BigDecimalValue[] { new BigDecimalValue(BigDecimal.valueOf(4)),
                new BigDecimalValue(BigDecimal.valueOf(5)),
                new BigDecimalValue(BigDecimal.valueOf(1)),
                new BigDecimalValue(BigDecimal.valueOf(7)) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((BigDecimalValue[]) null));
        assertEquals(null, MathUtils.median(new BigDecimalValue[0]));

        values = new BigDecimalValue[] { null,
                new BigDecimalValue(BigDecimal.valueOf(4)),
                new BigDecimalValue(BigDecimal.valueOf(4)),
                null,
                new BigDecimalValue(BigDecimal.valueOf(1)),
                new BigDecimalValue(BigDecimal.valueOf(7)),
                null,
                new BigDecimalValue(BigDecimal.valueOf(2)) };
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new BigDecimalValue[] { null,
                new BigDecimalValue(BigDecimal.valueOf(4)),
                new BigDecimalValue(BigDecimal.valueOf(5)),
                null,
                new BigDecimalValue(BigDecimal.valueOf(1)),
                null,
                new BigDecimalValue(BigDecimal.valueOf(7)) };
        assertEquals(4.5d, MathUtils.median(values), 0.1);

    }
}
