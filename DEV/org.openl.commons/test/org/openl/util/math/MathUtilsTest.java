package org.openl.util.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

/**
 * @author DLiauchuk
 */
public class MathUtilsTest {

    @Test
    public void testMedianByte() {
        byte[] values = new byte[]{4, 4, 1, 7, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new byte[]{4, 5, 1, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((byte[]) null));
        assertNull(MathUtils.median(new byte[0]));
    }

    @Test
    public void testMedianShort() {
        short[] values = new short[]{4, 4, 1, 7, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new short[]{4, 5, 1, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((short[]) null));
        assertNull(MathUtils.median(new short[0]));
    }

    @Test
    public void testMedianInt() {
        int[] values = new int[]{4, 4, 1, 7, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new int[]{4, 5, 1, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((int[]) null));
        assertNull(MathUtils.median(new int[0]));
    }

    @Test
    public void testMedianLong() {
        long[] values = new long[]{4, 4, 1, 7, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new long[]{4, 5, 1, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((long[]) null));
        assertNull(MathUtils.median(new long[0]));
    }

    @Test
    public void testMedianFloat() {
        float[] values = new float[]{4, 4, 1, 7, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new float[]{4, 5, 1, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((float[]) null));
        assertNull(MathUtils.median(new float[0]));
    }

    @Test
    public void testMedianFloatWrapper() {
        Float[] values = new Float[]{4f, 4f, 1f, 7f, 2f};
        assertTrue(MathUtils.median(values) instanceof Float);
        assertEquals(4.0F, MathUtils.median(values), 0.1);
        values = new Float[]{4f, 5f, 1f, 7f};
        assertEquals(4.5F, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((Float[]) null));
        assertNull(MathUtils.median(new Float[0]));

        values = new Float[]{null, 4f, 4f, null, 1f, 7f, null, 2f};
        assertEquals(4.0F, MathUtils.median(values), 0.1);
        values = new Float[]{null, 4f, 5f, null, 1f, null, 7f};
        assertEquals(4.5F, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianDouble() {
        double[] values = new double[]{4, 4, 1, 7, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new double[]{4, 5, 1, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((double[]) null));
        assertNull(MathUtils.median(new double[0]));
        values = new double[]{4};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new double[]{4, 5};
        assertEquals(4.5, MathUtils.median(values), 0.1);

    }

    @Test
    public void testMedianT() {
        Integer[] values = new Integer[]{4, 4, 1, 7, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new Integer[]{4, 5, 1, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);

        assertNull(MathUtils.median((Integer[]) null));
        assertNull(MathUtils.median(new Integer[0]));

        values = new Integer[]{null, 4, 4, null, 1, 7, null, 2};
        assertEquals(4.0, MathUtils.median(values), 0.1);
        values = new Integer[]{null, 4, 5, null, 1, null, 7};
        assertEquals(4.5, MathUtils.median(values), 0.1);
    }

    @Test
    public void testMedianBigInteger() {
        BigInteger[] values = new BigInteger[]{BigInteger
                .valueOf(4), BigInteger.valueOf(4), BigInteger.valueOf(1), BigInteger.valueOf(7), BigInteger.valueOf(2)};
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigInteger[]{BigInteger.valueOf(4),
                BigInteger.valueOf(5),
                BigInteger.valueOf(1),
                BigInteger.valueOf(7)};
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));

        assertNull(MathUtils.median((BigInteger[]) null));
        assertNull(MathUtils.median(new BigInteger[0]));

        values = new BigInteger[]{null,
                BigInteger.valueOf(4),
                BigInteger.valueOf(4),
                null,
                BigInteger.valueOf(1),
                null,
                BigInteger.valueOf(7),
                null,
                BigInteger.valueOf(2)};
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigInteger[]{null,
                BigInteger.valueOf(4),
                BigInteger.valueOf(5),
                null,
                BigInteger.valueOf(1),
                null,
                BigInteger.valueOf(7)};
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));
        values = new BigInteger[]{BigInteger.valueOf(4)};
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigInteger[]{BigInteger.valueOf(4), BigInteger.valueOf(5)};
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));
    }

    @Test
    public void testMedianBigDecimal() {
        BigDecimal[] values = new BigDecimal[]{BigDecimal
                .valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(1), BigDecimal.valueOf(7), BigDecimal.valueOf(2)};
        assertEquals(new BigDecimal(4.0), MathUtils.median(values));
        values = new BigDecimal[]{BigDecimal.valueOf(4),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(7)};
        assertEquals(new BigDecimal(4.5), MathUtils.median(values));

        assertNull(MathUtils.median((BigDecimal[]) null));
        assertNull(MathUtils.median(new BigDecimal[0]));

        values = new BigDecimal[]{null,
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(4),
                null,
                BigDecimal.valueOf(1),
                null,
                BigDecimal.valueOf(7),
                null,
                BigDecimal.valueOf(2)};
        assertEquals(BigDecimal.valueOf(4), MathUtils.median(values));
        values = new BigDecimal[]{null,
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(5),
                null,
                BigDecimal.valueOf(1),
                null,
                BigDecimal.valueOf(7)};
        assertEquals(BigDecimal.valueOf(4.5), MathUtils.median(values));
        values = new BigDecimal[]{BigDecimal.valueOf(4)};
        assertEquals(new BigDecimal(4), MathUtils.median(values));
        values = new BigDecimal[]{BigDecimal.valueOf(4), BigDecimal.valueOf(5)};
        assertEquals(new BigDecimal(4.5), MathUtils.median(values));
    }

    @Test
    public void testBigForByte() throws Throwable {
        byte[] values = new byte[]{10, 45, 4, 44, 22};
        assertEquals((byte)45, MathUtils.big(values, 1));
        assertEquals((byte)44, MathUtils.big(values, 2));
        assertEquals((byte)22, MathUtils.big(values, 3));
        assertEquals((byte)10, MathUtils.big(values, 4));
        assertEquals((byte)4, MathUtils.big(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 6);
        });
        assertNull(MathUtils.big((byte[])null, 1));
    }

    @Test
    public void testBigForShort() throws Throwable {
        short[] values = new short[]{10, 45, 4, 44, 22};
        assertEquals((short)45, MathUtils.big(values, 1));
        assertEquals((short)44, MathUtils.big(values, 2));
        assertEquals((short)22, MathUtils.big(values, 3));
        assertEquals((short)10, MathUtils.big(values, 4));
        assertEquals((short)4, MathUtils.big(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 6);
        });
        assertNull(MathUtils.big((short[])null, 1));
    }

    @Test
    public void testBigForInt() throws Throwable {
        int[] values = new int[]{10, 45, 4, 44, 22};
        assertEquals(45, MathUtils.big(values, 1));
        assertEquals(44, MathUtils.big(values, 2));
        assertEquals(22, MathUtils.big(values, 3));
        assertEquals(10, MathUtils.big(values, 4));
        assertEquals(4, MathUtils.big(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 6);
        });
        assertNull(MathUtils.big((int[])null, 1));
    }

    @Test
    public void testBigForLong() throws Throwable {
        long[] values = new long[]{10, 45, 4, 44, 22};
        assertEquals(45, MathUtils.big(values, 1));
        assertEquals(44, MathUtils.big(values, 2));
        assertEquals(22, MathUtils.big(values, 3));
        assertEquals(10, MathUtils.big(values, 4));
        assertEquals(4, MathUtils.big(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 6);
        });
        assertNull(MathUtils.big((long[])null, 1));
    }

    @Test
    public void testBigForFloat() throws Throwable {
        float[] values = new float[]{10, 45, 4, 44, 22};
        assertEquals(45, MathUtils.big(values, 1));
        assertEquals(44, MathUtils.big(values, 2));
        assertEquals(22, MathUtils.big(values, 3));
        assertEquals(10, MathUtils.big(values, 4));
        assertEquals(4, MathUtils.big(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 6);
        });
        assertNull(MathUtils.big((float[]) null, 1));
    }

    @Test
    public void testBigForDouble() throws Throwable {
        double[] values = new double[]{10, 45, 4, 44, 22};
        assertEquals(45, MathUtils.big(values, 1));
        assertEquals(44, MathUtils.big(values, 2));
        assertEquals(22, MathUtils.big(values, 3));
        assertEquals(10, MathUtils.big(values, 4));
        assertEquals(4, MathUtils.big(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.big(values, 6);
        });
        assertNull(MathUtils.big((double[]) null, 1));
    }

    @Test
    public void testBigForT() {
        Long[] mas = new Long[]{10L, 45L, 4L, 44L, 22L};
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

        mas = new Long[]{null, 10L, 45L, 4L, null, 44L, null, 22L};
        assertEquals(Long.valueOf(45), MathUtils.big(mas, 1));
        assertEquals(Long.valueOf(44), MathUtils.big(mas, 2));
        assertEquals(Long.valueOf(22), MathUtils.big(mas, 3));
        assertEquals(Long.valueOf(10), MathUtils.big(mas, 4));
        assertEquals(Long.valueOf(4), MathUtils.big(mas, 5));

    }

    @Test
    public void testSmallForByte() throws Throwable {
        byte[] values = new byte[]{10, 45, 4, 44, 22};
        assertEquals((byte)4, MathUtils.small(values, 1));
        assertEquals((byte)10, MathUtils.small(values, 2));
        assertEquals((byte)22, MathUtils.small(values, 3));
        assertEquals((byte)44, MathUtils.small(values, 4));
        assertEquals((byte)45, MathUtils.small(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 6);
        });
        assertNull(MathUtils.small((byte[]) null, 1));
    }

    @Test
    public void testSmallForShort() throws Throwable {
        short[] values = new short[]{10, 45, 4, 44, 22};
        assertEquals((short)4, MathUtils.small(values, 1));
        assertEquals((short)10, MathUtils.small(values, 2));
        assertEquals((short)22, MathUtils.small(values, 3));
        assertEquals((short)44, MathUtils.small(values, 4));
        assertEquals((short)45, MathUtils.small(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 6);
        });
        assertNull(MathUtils.small((short[]) null, 1));
    }

    @Test
    public void testSmallForInt() throws Throwable {
        int[] values = new int[]{10, 45, 4, 44, 22};
        assertEquals(4, MathUtils.small(values, 1));
        assertEquals(10, MathUtils.small(values, 2));
        assertEquals(22, MathUtils.small(values, 3));
        assertEquals(44, MathUtils.small(values, 4));
        assertEquals(45, MathUtils.small(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 6);
        });
        assertNull(MathUtils.small((int[]) null, 1));
    }

    @Test
    public void testSmallForLong() throws Throwable {
        long[] values = new long[]{10, 45, 4, 44, 22};
        assertEquals(4,  MathUtils.small(values, 1));
        assertEquals(10, MathUtils.small(values, 2));
        assertEquals(22, MathUtils.small(values, 3));
        assertEquals(44, MathUtils.small(values, 4));
        assertEquals(45, MathUtils.small(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 6);
        });
        assertNull(MathUtils.small((long[]) null, 1));
    }

    @Test
    public void testSmallForFloat() throws Throwable {
        float[] values = new float[]{10, 45, 4, 44, 22};
        assertEquals(4,  MathUtils.small(values, 1));
        assertEquals(10, MathUtils.small(values, 2));
        assertEquals(22, MathUtils.small(values, 3));
        assertEquals(44, MathUtils.small(values, 4));
        assertEquals(45, MathUtils.small(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 6);
        });
        assertNull(MathUtils.small((float[]) null, 1));
    }

    @Test
    public void testSmallForDouble() throws Throwable {
        double[] values = new double[]{10, 45, 4, 44, 22};
        assertEquals(4,  MathUtils.small(values, 1));
        assertEquals(10, MathUtils.small(values, 2));
        assertEquals(22, MathUtils.small(values, 3));
        assertEquals(44, MathUtils.small(values, 4));
        assertEquals(45, MathUtils.small(values, 5));
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MathUtils.small(values, 6);
        });
        assertNull(MathUtils.small((double[]) null, 1));
    }

    @Test
    public void testSmallForTWhenFirstArgIsNull() {
        Long[] mas = null;
        assertNull(MathUtils.small(mas, 1));
    }

    @Test
    public void testSmallForT() {
        Long[] mas = new Long[]{10L, 45L, 4L, 44L, 22L};
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

        mas = new Long[]{null, 10L, 45L, 4L, null, 44L, null, 22L};
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
        arr = new byte[]{1, 2, 3};
        assertEquals(Byte.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForShort() {
        short[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new short[]{1, 2, 3};
        assertEquals(Short.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForInt() {
        int[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new int[]{1, 2, 3};
        assertEquals(Integer.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForLong() {
        long[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new long[]{1, 2, 3};
        assertEquals(Long.valueOf("6"), MathUtils.sum(arr));
    }

    @Test
    public void testSumForFloat() {
        float[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new float[]{1.1f, 2.2f, 3.3f};
        assertEquals(Float.parseFloat("6.6"), MathUtils.sum(arr), 0.1);
    }

    @Test
    public void testSumForDouble() {
        double[] arr = null;
        assertNull(MathUtils.sum(arr));
        arr = new double[]{1.1d, 2.2d, 3.3d};
        assertEquals(Double.parseDouble("6.6"), MathUtils.sum(arr), 0.1);
    }
}
