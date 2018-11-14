package org.openl.util.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author DLiauchuk TODO: test all methods.
 */
public class MathUtilsTest {

    @Test
    public void testMedianByte() {
        byte[] values = new byte[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new byte[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((byte[]) null));
        assertEquals(null, MathUtils.median(new byte[0]));
    }

    @Test
    public void testMedianShort() {
        short[] values = new short[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new short[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((short[]) null));
        assertEquals(null, MathUtils.median(new short[0]));
    }

    @Test
    public void testMedianInt() {
        int[] values = new int[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new int[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((int[]) null));
        assertEquals(null, MathUtils.median(new int[0]));
    }

    @Test
    public void testMedianLong() {
        long[] values = new long[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new long[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((long[]) null));
        assertEquals(null, MathUtils.median(new long[0]));
    }

    @Test
    public void testMedianFloat() {
        float[] values = new float[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new float[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((float[]) null));
        assertEquals(null, MathUtils.median(new float[0]));
    }

    @Test
    public void testMedianFloatWrapper() {
        Float[] values = new Float[] { 4f, 4f, 1f, 7f, 2f };
        assertTrue(MathUtils.median(values) instanceof Float);
        assertEquals(new Float(4.0), MathUtils.median(values), 0.1);
        values = new Float[] { 4f, 5f, 1f, 7f };
        assertEquals(new Float(4.5), MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((Float[]) null));
        assertEquals(null, MathUtils.median(new Float[0]));

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

        assertEquals(null, MathUtils.median((double[]) null));
        assertEquals(null, MathUtils.median(new double[0]));

    }

    @Test
    public void testMedianT() {
        Integer[] values = new Integer[] { 4, 4, 1, 7, 2 };
        assertEquals(new Double(4.0), MathUtils.median(values), 0.1);
        values = new Integer[] { 4, 5, 1, 7 };
        assertEquals(new Double(4.5), MathUtils.median(values), 0.1);

        assertEquals(null, MathUtils.median((Integer[]) null));
        assertEquals(null, MathUtils.median(new Integer[0]));

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

        assertEquals(null, MathUtils.median((BigInteger[]) null));
        assertEquals(null, MathUtils.median(new BigInteger[0]));

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

        assertEquals(null, MathUtils.median((BigDecimal[]) null));
        assertEquals(null, MathUtils.median(new BigDecimal[0]));

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

        try {
            assertEquals(0, MathUtils.quotient(BigDecimal.valueOf(3.22), BigDecimal.valueOf(0)));
            fail();
        } catch (ArithmeticException e) {
            assertTrue(true);
        }
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
    public void testModFloat() {
        assertEquals(1.47, MathUtils.mod((float) 3.22, (float) 1.75), 0.01);
    }

    @Test
    public void testModDouble() {
        assertEquals(1.47, MathUtils.mod(3.22, 1.75), 0.01);
    }

    @Test
    public void testModBigDecimal() {
        assertEquals(BigDecimal.valueOf(1.47), MathUtils.mod(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(0), MathUtils.mod(null, BigDecimal.valueOf(1.75)));
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
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }

        assertEquals(null, smallMethod.invoke(null, null, 5));

        try {
            try {
                smallMethod.invoke(null, values, 0);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array", e.getMessage());
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
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }

        assertEquals(null, bigMethod.invoke(null, null, 5));

        try {
            try {
                bigMethod.invoke(null, values, 0);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array", e.getMessage());
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
        Long[] mas = new Long[] { Long
            .valueOf(10), Long.valueOf(45), Long.valueOf(4), Long.valueOf(44), Long.valueOf(22) };
        assertEquals(Long.valueOf(45), MathUtils.big(mas, 1));
        assertEquals(Long.valueOf(44), MathUtils.big(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.big(mas, 3));
        assertEquals(Long.valueOf(10), MathUtils.big(mas, 4));
        assertEquals(Long.valueOf(4), MathUtils.big(mas, 5));

        try {
            MathUtils.big(mas, 6);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }

        mas = null;
        assertEquals(null, MathUtils.big(mas, 5));

        mas = new Long[1];
        try {
            MathUtils.big(mas, 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array", e.getMessage());
        }

        mas = new Long[] { null,
                Long.valueOf(10),
                Long.valueOf(45),
                Long.valueOf(4),
                null,
                Long.valueOf(44),
                null,
                Long.valueOf(22) };
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
    public void testSmallForT() {
        Long[] mas = new Long[] { Long
            .valueOf(10), Long.valueOf(45), Long.valueOf(4), Long.valueOf(44), Long.valueOf(22) };
        assertEquals(Long.valueOf(4), MathUtils.small(mas, 1));
        assertEquals(Long.valueOf(10), MathUtils.small(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.small(mas, 3));
        assertEquals(Long.valueOf(44), MathUtils.small(mas, 4));
        assertEquals(Long.valueOf(45), MathUtils.small(mas, 5));

        try {
            MathUtils.big(mas, 6);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }

        mas = null;
        assertEquals(null, MathUtils.big(mas, 5));

        mas = new Long[1];
        try {
            MathUtils.big(mas, 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '0' in the given array", e.getMessage());
        }

        mas = new Long[] { null,
                Long.valueOf(10),
                Long.valueOf(45),
                Long.valueOf(4),
                null,
                Long.valueOf(44),
                null,
                Long.valueOf(22) };
        assertEquals(Long.valueOf(4), MathUtils.small(mas, 1));
        assertEquals(Long.valueOf(10), MathUtils.small(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.small(mas, 3));
        assertEquals(Long.valueOf(44), MathUtils.small(mas, 4));
        assertEquals(Long.valueOf(45), MathUtils.small(mas, 5));
    }

}
