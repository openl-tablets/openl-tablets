package org.openl.util.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;

/**
 * @author DLiauchuk TODO: test all methods.
 */
public class MathUtilsTest {
    @Test
    public void testAvgByte() {
        byte[] byteArray = getTestByteArray();
        assertEquals(new Double(5.333333d), MathUtils.avg(byteArray), 0.01);

        byte[] nullArray = null;
        assertEquals(null, MathUtils.avg(nullArray));

        byte[] emptyArray = new byte[1];
        assertEquals(new Double(0), MathUtils.avg(emptyArray));
    }

    @Test
    public void testAvgShort() {
        short[] shortArray = new short[] { 2, 3, 4 };
        assertEquals(new Double(3d), MathUtils.avg(shortArray));

        short[] nullArray = null;
        assertEquals(null, MathUtils.avg(nullArray));

        short[] emptyArray = new short[1];
        assertEquals(new Double(0), MathUtils.avg(emptyArray));
    }

    @Test
    public void testAvgBigInteger() {
        BigInteger[] bigIntegerArray = new BigInteger[] { BigInteger.valueOf(2),
                BigInteger.valueOf(3),
                BigInteger.valueOf(4) };
        assertEquals(BigDecimal.valueOf(3), MathUtils.avg(bigIntegerArray));

        BigInteger[] nullArray = null;
        assertEquals(null, MathUtils.avg(nullArray));

        BigInteger[] emptyArray = new BigInteger[1];
        assertEquals(null, MathUtils.avg(emptyArray));
    }

    @Test
    public void testMedianByte() {
        byte[] byteArray = getTestByteArray();
        assertEquals(new Double(5.0), MathUtils.median(byteArray), 0.1);
    }

    @Test
    public void testMedianFloat() {
        float[] floatArray = new float[] { (float) 10.5, (float) 7.2 };
        assertEquals((float) 8.85, (float) MathUtils.median(floatArray), 0.01);
    }

    @Test
    public void testSumLong() {
        long[] array = new long[] { 3, 4 };
        assertEquals(Long.valueOf(7), MathUtils.sum(array));
        
        Long[] array1 = new Long[] { null, null, null };
        assertEquals(null, MathUtils.sum(array1));
        
        Long[] array2 = new Long[] { null, 2l, null };
        assertEquals(Long.valueOf(2l), MathUtils.sum(array2));
    }
    
    @Test
    public void testSumDouble() {
        double[] doubleArray = new double[] { 3, 4, 4.5, -6.78 };
        assertEquals(4.72d, MathUtils.sum(doubleArray), 0.01);
        
        Double[] doubleArray1 = new Double[] { null, null, null };
        assertEquals(null, MathUtils.sum(doubleArray1));
        
        Double[] doubleArray2 = new Double[] { null, 2.1, null };
        assertEquals(Double.valueOf(2.1), MathUtils.sum(doubleArray2), 0.01d);
    }

    @Test
    public void testProductLong() {
        long[] array = new long[] { 3, 4 };
        assertEquals(Long.valueOf(12), MathUtils.product(array));
        
        Long[] array1 = new Long[] { null, null, null };
        assertEquals(null, MathUtils.product(array1));
        
        Long[] array2 = new Long[] { null, 2l, null };
        assertEquals(Long.valueOf(2), MathUtils.product(array2));
    }

    @Test
    public void testProductDouble() {
        double[] doubleArray = new double[] { 3, 4, 4.5, -6.78 };
        assertEquals(-366.12, MathUtils.product(doubleArray), 0.01);
        
        Double[] doubleArray1 = new Double[] { null, null, null };
        assertEquals(null, MathUtils.product(doubleArray1));
        
        Double[] doubleArray2 = new Double[] { null, 2.1, null };
        assertEquals(Double.valueOf(2.1), MathUtils.product(doubleArray2), 0.01d);
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

    @Test
    public void testSmallInt() {
        int[] intMas = new int[] { 10, 17, 13, 44, 1 };
        assertEquals(Integer.valueOf(1), MathUtils.small(intMas, 1));
        assertEquals(Integer.valueOf(10), MathUtils.small(intMas, 2));
        assertEquals(Integer.valueOf(13), MathUtils.small(intMas, 3));
        assertEquals(Integer.valueOf(44), MathUtils.small(intMas, 5));
        try {
            assertEquals(Integer.valueOf(0), MathUtils.small(intMas, 6));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }

        intMas = null;
        assertEquals(null, MathUtils.small(intMas, 5));

        intMas = new int[1];
        try {
            assertEquals(Integer.valueOf(0), MathUtils.small(intMas, 5));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '5' in the given array", e.getMessage());
        }
    }

    @Test
    public void testBigForInt() {
        int[] mas = new int[] { 10, 45, 4, 44, 22 };
        assertEquals(Integer.valueOf(45), MathUtils.big(mas, 1));
        assertEquals(Integer.valueOf(44), MathUtils.big(mas, 2));
        assertEquals(Integer.valueOf(22), MathUtils.big(mas, 3));
        assertEquals(Integer.valueOf(10), MathUtils.big(mas, 4));
        assertEquals(Integer.valueOf(4), MathUtils.big(mas, 5));

        try {
            assertEquals(Integer.valueOf(0), MathUtils.big(mas, 6));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }

        mas = null;
        assertEquals(null, MathUtils.big(mas, 5));

        mas = new int[1];
        try {
            assertEquals(Integer.valueOf(0), MathUtils.big(mas, 5));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '5' in the given array", e.getMessage());
        }
    }

    @Test
    public void testBigForLong() {
        Long[] mas = new Long[] { Long
            .valueOf(10), Long.valueOf(45), Long.valueOf(4), Long.valueOf(44), Long.valueOf(22) };
        assertEquals(Long.valueOf(45), MathUtils.big(mas, 1));
        assertEquals(Long.valueOf(44), MathUtils.big(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.big(mas, 3));
        assertEquals(Long.valueOf(10), MathUtils.big(mas, 4));
        assertEquals(Long.valueOf(4), MathUtils.big(mas, 5));

        try {
            assertEquals(Long.valueOf(0), MathUtils.big(mas, 6));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '6' in the given array", e.getMessage());
        }

        mas = null;
        assertEquals(null, MathUtils.big(mas, 5));

        mas = new Long[1];
        try {
            assertEquals(Long.valueOf(0), MathUtils.big(mas, 5));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("There is no position '5' in the given array", e.getMessage());
        }
    }

    private byte[] getTestByteArray() {
        return new byte[] { 3, 5, 8 };
    }
}
