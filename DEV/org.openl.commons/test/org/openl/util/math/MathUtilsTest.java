package org.openl.util.math;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author DLiauchuk
 */
public class MathUtilsTest {

    @Test
    public void testMedianByte() {
        byte[] values = new byte[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new byte[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((byte[]) null));
        assertNull(MathUtils.median(new byte[0]));
    }

    @Test
    public void testMedianShort() {
        short[] values = new short[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new short[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((short[]) null));
        assertNull(MathUtils.median(new short[0]));
    }

    @Test
    public void testMedianInt() {
        int[] values = new int[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new int[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((int[]) null));
        assertNull(MathUtils.median(new int[0]));
    }

    @Test
    public void testMedianLong() {
        long[] values = new long[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new long[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((long[]) null));
        assertNull(MathUtils.median(new long[0]));
    }

    @Test
    public void testMedianFloat() {
        float[] values = new float[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new float[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((float[]) null));
        assertNull(MathUtils.median(new float[0]));
    }

    @Test
    public void testMedianFloatWrapper() {
        Float[] values = new Float[] { 4f, 4f, 1f, 7f, 2f };
        assertTrue(MathUtils.median(values) instanceof Float);
        assertEquals(new Float(4.0), MathUtils.median(values), 0.1);
        values = new Float[] { 4f, 5f, 1f, 7f };
        assertEquals(new Float(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((Float[]) null));
        assertNull(MathUtils.median(new Float[0]));

        values = new Float[] { null, 4f, 4f, null, 1f, 7f, null, 2f };
        assertEquals(new Float(4.0), MathUtils.median(values), 0.1);
        values = new Float[] { null, 4f, 5f, null, 1f, null, 7f };
        assertEquals(new Float(4.5), MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianDouble() {
        double[] values = new double[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new double[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((double[]) null));
        assertNull(MathUtils.median(new double[0]));
        values = new double[] { 4 };
        assertEquals(new Double(4), MathUtils.median(values), 0.1);
        values = new double[] { 4, 5 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianT() {
        Integer[] values = new Integer[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new Integer[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((Integer[]) null));
        assertNull(MathUtils.median(new Integer[0]));

        values = new Integer[] { null, 4, 4, null, 1, 7, null, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new Integer[] { null, 4, 5, null, 1, null, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);
    }

    @Test
    public void testMedianBigInteger() {
        BigInteger[] values = new BigInteger[] { BigInteger
            .valueOf(4), BigInteger.valueOf(4), BigInteger.valueOf(1), BigInteger.valueOf(7), BigInteger.valueOf(2) };
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigInteger[] { BigInteger.valueOf(4),
                BigInteger.valueOf(5),
                BigInteger.valueOf(1),
                BigInteger.valueOf(7) };
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));

        assertNull(MathUtils.median((BigInteger[]) null));
        assertNull(MathUtils.median(new BigInteger[0]));

        values = new BigInteger[] { null,
                BigInteger.valueOf(4),
                BigInteger.valueOf(4),
                null,
                BigInteger.valueOf(1),
                null,
                BigInteger.valueOf(7),
                null,
                BigInteger.valueOf(2) };
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigInteger[] { null,
                BigInteger.valueOf(4),
                BigInteger.valueOf(5),
                null,
                BigInteger.valueOf(1),
                null,
                BigInteger.valueOf(7) };
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));
        values = new BigInteger[] { BigInteger.valueOf(4) };
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigInteger[] { BigInteger.valueOf(4), BigInteger.valueOf(5) };
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));
    }

    @Test
    public void testMedianBigDecimal() {
        BigDecimal[] values = new BigDecimal[] { BigDecimal
            .valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(1), BigDecimal.valueOf(7), BigDecimal.valueOf(2) };
        assertEquals(new BigDecimal(4.0), MathUtils.median(values));
        values = new BigDecimal[] { BigDecimal.valueOf(4),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(7) };
        assertEquals(new BigDecimal(4.5), MathUtils.median(values));

        assertNull(MathUtils.median((BigDecimal[]) null));
        assertNull(MathUtils.median(new BigDecimal[0]));

        values = new BigDecimal[] { null,
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(4),
                null,
                BigDecimal.valueOf(1),
                null,
                BigDecimal.valueOf(7),
                null,
                BigDecimal.valueOf(2) };
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigDecimal[] { null,
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(5),
                null,
                BigDecimal.valueOf(1),
                null,
                BigDecimal.valueOf(7) };
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));
        values = new BigDecimal[] { BigDecimal.valueOf(4) };
        assertEquals(new BigDecimal(4), MathUtils.median(values));
        values = new BigDecimal[] { BigDecimal.valueOf(4), BigDecimal.valueOf(5) };
        assertEquals(new BigDecimal(4.5), MathUtils.median(values));
    }

    @Test
    public void testQuaotientDouble() {
        assertEquals(1, MathUtils.quotient(3.22, 1.75));
    }

    @Test
    public void testQuaotientBigDecimal() {
        assertEquals(1, MathUtils.quotient(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));

        BigDecimal nullObj = null;
        assertEquals(0, MathUtils.quotient(nullObj, nullObj));
    }

    @Test(expected = ArithmeticException.class)
    public void testQuaotientBigDecimalOfZero() {
        MathUtils.quotient(BigDecimal.valueOf(3.22), BigDecimal.valueOf(0));
    }

    @Test
    public void testQuotientByteWrapper() {
        assertEquals(0, MathUtils.quotient(null, Byte.valueOf("13")));
        assertEquals(0, MathUtils.quotient(Byte.valueOf("13"), null));
        assertEquals(3, MathUtils.quotient(Byte.valueOf("19"), Byte.valueOf("5")));
        assertEquals(-3, MathUtils.quotient(Byte.valueOf("19"), Byte.valueOf("-5")));
    }

    @Test
    public void testQuotientShortWrapper() {
        assertEquals(0, MathUtils.quotient(null, Short.valueOf("13")));
        assertEquals(0, MathUtils.quotient(Short.valueOf("13"), null));
        assertEquals(3, MathUtils.quotient(Short.valueOf("19"), Short.valueOf("5")));
        assertEquals(-3, MathUtils.quotient(Short.valueOf("19"), Short.valueOf("-5")));
    }

    @Test
    public void testQuotientIntegerWrapper() {
        assertEquals(0, MathUtils.quotient(null, Integer.valueOf("13")));
        assertEquals(0, MathUtils.quotient(Integer.valueOf("13"), null));
        assertEquals(3, MathUtils.quotient(Integer.valueOf("19"), Integer.valueOf("5")));
        assertEquals(-3, MathUtils.quotient(Integer.valueOf("19"), Integer.valueOf("-5")));
    }

    @Test
    public void testQuotientLongWrapper() {
        assertEquals(0, MathUtils.quotient(null, Long.valueOf("13")));
        assertEquals(0, MathUtils.quotient(Long.valueOf("13"), null));
        assertEquals(3, MathUtils.quotient(Long.valueOf("19"), Long.valueOf("5")));
        assertEquals(-3, MathUtils.quotient(Long.valueOf("19"), Long.valueOf("-5")));
    }

    @Test
    public void testQuotientFloatWrapper() {
        assertEquals(0, MathUtils.quotient(null, Float.valueOf("13")));
        assertEquals(0, MathUtils.quotient(Float.valueOf("13"), null));
        assertEquals(3, MathUtils.quotient(Float.valueOf("19"), Float.valueOf("5.55")));
        assertEquals(-3, MathUtils.quotient(Float.valueOf("19"), Float.valueOf("-5.55")));
    }

    @Test
    public void testQuotientDoubleWrapper() {
        assertEquals(0, MathUtils.quotient(null, Double.valueOf("13")));
        assertEquals(0, MathUtils.quotient(Double.valueOf("13"), null));
        assertEquals(3, MathUtils.quotient(Double.valueOf("19"), Double.valueOf("5.55")));
        assertEquals(-3, MathUtils.quotient(Double.valueOf("19"), Double.valueOf("-5.55")));
    }

    @Test
    public void testQuotientBigInteger() {
        assertEquals(0, MathUtils.quotient(null, BigInteger.valueOf(13)));
        assertEquals(0, MathUtils.quotient(BigInteger.valueOf(13), null));
        assertEquals(3, MathUtils.quotient(BigInteger.valueOf(19), BigInteger.valueOf(5)));
        assertEquals(-3, MathUtils.quotient(BigInteger.valueOf(19), BigInteger.valueOf(-5)));
    }

    @Test
    public void testModByte() {
        assertEquals(6, MathUtils.mod((byte) 19, (byte) 13));
        assertEquals(-7, MathUtils.mod((byte) 19, (byte) -13));
        assertEquals(7, MathUtils.mod((byte) -19, (byte) 13));
        assertEquals(-6, MathUtils.mod((byte) -19, (byte) -13));
        assertEquals(0, MathUtils.mod((byte) 19, (byte) 19));
        assertEquals(0, MathUtils.mod((byte) 0, (byte) 19));
    }

    @Test
    public void testModShort() {
        assertEquals(6, MathUtils.mod((short) 19, (short) 13));
        assertEquals(-7, MathUtils.mod((short) 19, (short) -13));
        assertEquals(7, MathUtils.mod((short) -19, (short) 13));
        assertEquals(-6, MathUtils.mod((short) -19, (short) -13));
        assertEquals(0, MathUtils.mod((short) 19, (short) 19));
        assertEquals(0, MathUtils.mod((short) 0, (short) 19));
    }

    @Test
    public void testModInt() {
        assertEquals(6, MathUtils.mod(19, 13));
        assertEquals(-7, MathUtils.mod(19, -13));
        assertEquals(7, MathUtils.mod(-19, 13));
        assertEquals(-6, MathUtils.mod(-19, -13));
        assertEquals(0, MathUtils.mod(19, 19));
        assertEquals(0, MathUtils.mod(0, 19));
    }

    @Test
    public void testModLong() {
        assertEquals(6, MathUtils.mod(19L, 13L));
        assertEquals(-7, MathUtils.mod(19L, -13L));
        assertEquals(7, MathUtils.mod(-19L, 13L));
        assertEquals(-6, MathUtils.mod(-19L, -13L));
        assertEquals(0, MathUtils.mod(19L, 19L));
        assertEquals(0, MathUtils.mod(0L, 19L));
    }

    @Test
    public void testModFloat() {
        assertEquals(1.47, MathUtils.mod((float) 3.22, (float) 1.75), 0.01);
        assertEquals(-0.28, MathUtils.mod((float) 3.22, (float) -1.75), 0.01);
    }

    @Test
    public void testModDouble() {
        assertEquals(1.47, MathUtils.mod(3.22, 1.75), 0.01);
        assertEquals(-0.28D, MathUtils.mod(3.22, -1.75), 0.01);
    }

    @Test
    public void testModByteWrapper() {
        assertEquals(Byte.valueOf("0"), MathUtils.mod(null, Byte.valueOf("13")));
        assertEquals(Byte.valueOf("0"), MathUtils.mod(Byte.valueOf("13"), null));
        assertEquals(Byte.valueOf("6"), MathUtils.mod(Byte.valueOf("19"), Byte.valueOf("13")));
        assertEquals(Byte.valueOf("-7"), MathUtils.mod(Byte.valueOf("19"), Byte.valueOf("-13")));
    }

    @Test
    public void testModShortWrapper() {
        assertEquals(Short.valueOf("0"), MathUtils.mod(null, Short.valueOf("13")));
        assertEquals(Short.valueOf("0"), MathUtils.mod(Short.valueOf("13"), null));
        assertEquals(Short.valueOf("6"), MathUtils.mod(Short.valueOf("19"), Short.valueOf("13")));
        assertEquals(Short.valueOf("-7"), MathUtils.mod(Short.valueOf("19"), Short.valueOf("-13")));
    }

    @Test
    public void testModIntegerWrapper() {
        assertEquals(Integer.valueOf("0"), MathUtils.mod(null, Integer.valueOf("13")));
        assertEquals(Integer.valueOf("0"), MathUtils.mod(Integer.valueOf("13"), null));
        assertEquals(Integer.valueOf("6"), MathUtils.mod(Integer.valueOf("19"), Integer.valueOf("13")));
        assertEquals(Integer.valueOf("-7"), MathUtils.mod(Integer.valueOf("19"), Integer.valueOf("-13")));
    }

    @Test
    public void testModLongWrapper() {
        assertEquals(Long.valueOf("0"), MathUtils.mod(null, Long.valueOf("13")));
        assertEquals(Long.valueOf("0"), MathUtils.mod(Long.valueOf("13"), null));
        assertEquals(Long.valueOf("6"), MathUtils.mod(Long.valueOf("19"), Long.valueOf("13")));
        assertEquals(Long.valueOf("-7"), MathUtils.mod(Long.valueOf("19"), Long.valueOf("-13")));
    }

    @Test
    public void testModFloatWrapper() {
        assertEquals(Float.valueOf("0"), MathUtils.mod(null, Float.valueOf("13")));
        assertEquals(Float.valueOf("0"), MathUtils.mod(Float.valueOf("13"), null));
        assertEquals(Float.valueOf("1.47"), MathUtils.mod(Float.valueOf("3.22"), Float.valueOf("1.75")), 0.01);
        assertEquals(Float.valueOf("-0.28"), MathUtils.mod(Float.valueOf("3.22"), Float.valueOf("-1.75")), 0.01);
    }

    @Test
    public void testModDoubleWrapper() {
        assertEquals(Double.valueOf("0"), MathUtils.mod(null, Double.valueOf("13")));
        assertEquals(Double.valueOf("0"), MathUtils.mod(Double.valueOf("13"), null));
        assertEquals(Double.valueOf("1.47"), MathUtils.mod(Double.valueOf("3.22"), Double.valueOf("1.75")), 0.01);
        assertEquals(Double.valueOf("-0.28"), MathUtils.mod(Double.valueOf("3.22"), Double.valueOf("-1.75")), 0.01);
    }

    @Test
    public void testModBigInteger() {
        assertEquals(BigInteger.valueOf(0), MathUtils.mod(null, BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(0), MathUtils.mod(BigInteger.valueOf(13), null));
        assertEquals(BigInteger.valueOf(6), MathUtils.mod(BigInteger.valueOf(19), BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(-7), MathUtils.mod(BigInteger.valueOf(19), BigInteger.valueOf(-13)));
    }

    @Test
    public void testModBigDecimal() {
        assertEquals(BigDecimal.valueOf(1.47), MathUtils.mod(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(0), MathUtils.mod(null, BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(0), MathUtils.mod(BigDecimal.valueOf(1.75), null));
        assertEquals(BigDecimal.valueOf(-0.28D), MathUtils.mod(BigDecimal.valueOf(3.22), BigDecimal.valueOf(-1.75)));
    }

    private void testSmall(Class<?> primitiveType, Object values) throws Throwable {
        Method smallMethod = MathUtils.class.getDeclaredMethod("small", values.getClass(), int.class);
        Method sortMethod = Arrays.class.getDeclaredMethod("sort", values.getClass());
        Method cloneMethod = Object.class.getDeclaredMethod("clone");
        cloneMethod.setAccessible(true);
        Object sortedValues = cloneMethod.invoke(values);
        sortMethod.invoke(null, sortedValues);

        assertEquals(Array.get(sortedValues, 0), smallMethod.invoke(null, values, 1));
        assertEquals(Array.get(sortedValues, 1), smallMethod.invoke(null, values, 2));
        assertEquals(Array.get(sortedValues, 2), smallMethod.invoke(null, values, 3));
        assertEquals(Array.get(sortedValues, 3), smallMethod.invoke(null, values, 4));
        assertEquals(Array.get(sortedValues, 4), smallMethod.invoke(null, values, 5));

        try {
            try {
                smallMethod.invoke(null, values, 6);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array.", e.getMessage());
        }

        assertNull(smallMethod.invoke(null, null, 5));

        try {
            try {
                smallMethod.invoke(null, values, 0);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array.", e.getMessage());
        }

    }

    private void testBig(Class<?> primitiveType, Object values) throws Throwable {
        Method bigMethod = MathUtils.class.getDeclaredMethod("big", values.getClass(), int.class);
        Method sortMethod = Arrays.class.getDeclaredMethod("sort", values.getClass());
        Method cloneMethod = Object.class.getDeclaredMethod("clone");
        cloneMethod.setAccessible(true);
        Object sortedValues = cloneMethod.invoke(values);
        sortMethod.invoke(null, sortedValues);

        assertEquals(Array.get(sortedValues, 4), bigMethod.invoke(null, values, 1));
        assertEquals(Array.get(sortedValues, 3), bigMethod.invoke(null, values, 2));
        assertEquals(Array.get(sortedValues, 2), bigMethod.invoke(null, values, 3));
        assertEquals(Array.get(sortedValues, 1), bigMethod.invoke(null, values, 4));
        assertEquals(Array.get(sortedValues, 0), bigMethod.invoke(null, values, 5));

        try {
            try {
                bigMethod.invoke(null, values, 6);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array.", e.getMessage());
        }

        assertNull(bigMethod.invoke(null, null, 5));

        try {
            try {
                bigMethod.invoke(null, values, 0);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array.", e.getMessage());
        }

    }

    @Test
    public void testBigForByte() throws Throwable {
        byte[] values = new byte[] { 10, 45, 4, 44, 22 };
        testBig(byte.class, values);
    }

    @Test
    public void testBigForShort() throws Throwable {
        short[] values = new short[] { 10, 45, 4, 44, 22 };
        testBig(short.class, values);
    }

    @Test
    public void testBigForInt() throws Throwable {
        int[] values = new int[] { 10, 45, 4, 44, 22 };
        testBig(int.class, values);
    }

    @Test
    public void testBigForLong() throws Throwable {
        long[] values = new long[] { 10, 45, 4, 44, 22 };
        testBig(long.class, values);
    }

    @Test
    public void testBigForFloat() throws Throwable {
        float[] values = new float[] { 10, 45, 4, 44, 22 };
        testBig(float.class, values);
    }

    @Test
    public void testBigForDouble() throws Throwable {
        double[] values = new double[] { 10, 45, 4, 44, 22 };
        testBig(double.class, values);
    }

    @Test
    public void testBigForT() {
        Long[] mas = new Long[] { 10L, 45L, 4L, 44L, 22L };
        assertEquals(Long.valueOf(45), MathUtils.big(mas, 1));
        assertEquals(Long.valueOf(44), MathUtils.big(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.big(mas, 3));
        assertEquals(Long.valueOf(10), MathUtils.big(mas, 4));
        assertEquals(Long.valueOf(4), MathUtils.big(mas, 5));

        try {
            MathUtils.big(mas, 6);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array.", e.getMessage());
        }

        mas = null;
        assertNull(MathUtils.big(mas, 5));

        mas = new Long[1];
        try {
            MathUtils.big(mas, 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array.", e.getMessage());
        }

        mas = new Long[] { null, 10L, 45L, 4L, null, 44L, null, 22L };
        assertEquals(Long.valueOf(45), MathUtils.big(mas, 1));
        assertEquals(Long.valueOf(44), MathUtils.big(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.big(mas, 3));
        assertEquals(Long.valueOf(10), MathUtils.big(mas, 4));
        assertEquals(Long.valueOf(4), MathUtils.big(mas, 5));

    }

    @Test
    public void testSmallForByte() throws Throwable {
        byte[] values = new byte[] { 10, 45, 4, 44, 22 };
        testSmall(byte.class, values);
    }

    @Test
    public void testSmallForShort() throws Throwable {
        short[] values = new short[] { 10, 45, 4, 44, 22 };
        testSmall(short.class, values);
    }

    @Test
    public void testSmallForInt() throws Throwable {
        int[] values = new int[] { 10, 45, 4, 44, 22 };
        testSmall(int.class, values);
    }

    @Test
    public void testSmallForLong() throws Throwable {
        long[] values = new long[] { 10, 45, 4, 44, 22 };
        testSmall(long.class, values);
    }

    @Test
    public void testSmallForFloat() throws Throwable {
        float[] values = new float[] { 10, 45, 4, 44, 22 };
        testSmall(float.class, values);
    }

    @Test
    public void testSmallForDouble() throws Throwable {
        double[] values = new double[] { 10, 45, 4, 44, 22 };
        testSmall(double.class, values);
    }

    @Test
    public void testSmallForTWhenFirstArgIsNull() {
        Long[] mas = null;
        assertNull(MathUtils.small(mas, 1));
    }

    @Test
    public void testSmallForT() {
        Long[] mas = new Long[] { 10L, 45L, 4L, 44L, 22L };
        assertEquals(Long.valueOf(4), MathUtils.small(mas, 1));
        assertEquals(Long.valueOf(10), MathUtils.small(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.small(mas, 3));
        assertEquals(Long.valueOf(44), MathUtils.small(mas, 4));
        assertEquals(Long.valueOf(45), MathUtils.small(mas, 5));

        try {
            MathUtils.small(mas, 6);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array.", e.getMessage());
        }

        mas = null;
        assertNull(MathUtils.small(mas, 5));

        mas = new Long[1];
        try {
            MathUtils.small(mas, 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array.", e.getMessage());
        }

        mas = new Long[] { null, 10L, 45L, 4L, null, 44L, null, 22L };
        assertEquals(Long.valueOf(4), MathUtils.small(mas, 1));
        assertEquals(Long.valueOf(10), MathUtils.small(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.small(mas, 3));
        assertEquals(Long.valueOf(44), MathUtils.small(mas, 4));
        assertEquals(Long.valueOf(45), MathUtils.small(mas, 5));
    }

    @Test
    public void testSumForByte() {
        byte[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new byte[] { 1, 2, 3 };
        assertEquals(Byte.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForShort() {
        short[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new short[] { 1, 2, 3 };
        assertEquals(Short.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForInt() {
        int[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new int[] { 1, 2, 3 };
        assertEquals(Integer.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForLong() {
        long[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new long[] { 1, 2, 3 };
        assertEquals(Long.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForFloat() {
        float[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new float[] { 1.1f, 2.2f, 3.3f };
        assertEquals(Float.valueOf("6.6"), MathUtils.sum(arr), 0.1);
    }

    @Test
    public void testSumForDouble() {
        double[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new double[] { 1.1d, 2.2d, 3.3d };
        assertEquals(Double.valueOf("6.6"), MathUtils.sum(arr), 0.1);
    }

    @Test
    public void testSortForT() {
        Long[] arr = null;
        assertNull(MathUtils.sort(arr));
        arr = new Long[] { 1L, 9L, 8L, 5L, 2L, 8L, 9L, 10L, 6L };
        Long[] expectedArr = new Long[] { 1L, 2L, 5L, 6L, 8L, 8L, 9L, 9L, 10L };
        assertArrayEquals(expectedArr, MathUtils.sort(arr));
    }

    @Test
    public void testSortForLong() {
        long[] arr = null;
        assertNull(MathUtils.sort(arr));
        arr = new long[] { 1L, 9L, 8L, 5L, 2L, 8L, 9L, 10L, 6L };
        long[] expectedArr = new long[] { 1L, 2L, 5L, 6L, 8L, 8L, 9L, 9L, 10L };
        assertArrayEquals(expectedArr, MathUtils.sort(arr));
    }

    @Test
    public void testSortForByte() {
        byte[] arr = null;
        assertNull(MathUtils.sort(arr));
        arr = new byte[] { 1, 9, 8, 5, 2, 8, 9, 10, 6 };
        byte[] expectedArr = new byte[] { 1, 2, 5, 6, 8, 8, 9, 9, 10 };
        assertArrayEquals(expectedArr, MathUtils.sort(arr));
    }

    @Test
    public void testSortForShort() {
        short[] arr = null;
        assertNull(MathUtils.sort(arr));
        arr = new short[] { 1, 9, 8, 5, 2, 8, 9, 10, 6 };
        short[] expectedArr = new short[] { 1, 2, 5, 6, 8, 8, 9, 9, 10 };
        assertArrayEquals(expectedArr, MathUtils.sort(arr));
    }

    @Test
    public void testSortForInt() {
        int[] arr = null;
        assertNull(MathUtils.sort(arr));
        arr = new int[] { 1, 9, 8, 5, 2, 8, 9, 10, 6 };
        int[] expectedArr = new int[] { 1, 2, 5, 6, 8, 8, 9, 9, 10 };
        assertArrayEquals(expectedArr, MathUtils.sort(arr));
    }

    @Test
    public void testSortForFloat() {
        float[] arr = null;
        assertNull(MathUtils.sort(arr));
        arr = new float[] { 1, 9, 8, 5, 2, 8, 9, 10, 6 };
        float[] expectedArr = new float[] { 1, 2, 5, 6, 8, 8, 9, 9, 10 };
        assertArrayEquals(expectedArr, MathUtils.sort(arr), 0.1f);
    }

    @Test
    public void testSortForDouble() {
        double[] arr = null;
        assertNull(MathUtils.sort(arr));
        arr = new double[] { 1, 9, 8, 5, 2, 8, 9, 10, 6 };
        double[] expectedArr = new double[] { 1, 2, 5, 6, 8, 8, 9, 9, 10 };
        assertArrayEquals(expectedArr, MathUtils.sort(arr), 0.1f);
    }

    @Test
    public void testDivideBigInteger() {
        assertNull(MathUtils.divide(null, BigInteger.valueOf(13)));
        assertNull(MathUtils.divide(BigInteger.valueOf(13), null));
        assertEquals(BigInteger.valueOf(3), MathUtils.divide(BigInteger.valueOf(19), BigInteger.valueOf(5)));
        assertEquals(BigInteger.valueOf(-3), MathUtils.divide(BigInteger.valueOf(19), BigInteger.valueOf(-5)));
    }

    @Test
    public void testDivideBigDecimal() {
        assertNull(MathUtils.divide(null, BigDecimal.valueOf(13)));
        assertNull(MathUtils.divide(BigDecimal.valueOf(13), null));
        assertEquals(BigDecimal.valueOf(3.8), MathUtils.divide(BigDecimal.valueOf(19), BigDecimal.valueOf(5)));
        assertEquals(BigDecimal.valueOf(-3.8), MathUtils.divide(BigDecimal.valueOf(19), BigDecimal.valueOf(-5)));
    }

}
