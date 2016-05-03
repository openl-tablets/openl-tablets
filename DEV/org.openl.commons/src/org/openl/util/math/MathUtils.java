package org.openl.util.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.math.NumberUtils;
import org.openl.util.ArrayTool;
import org.openl.util.ClassUtils;

/**
 * The biggest part of methods is being generated. See org.openl.rules.gen
 * module.
 * 
 * @author DLiauchuk
 * 
 */
public class MathUtils {

    // <<< INSERT Functions >>>
    // MAX function
    public static boolean max(byte value1, byte value2) {
        return value1 > value2;
    }

    public static boolean max(short value1, short value2) {
        return value1 > value2;
    }

    public static boolean max(int value1, int value2) {
        return value1 > value2;
    }

    public static boolean max(long value1, long value2) {
        return value1 > value2;
    }

    public static boolean max(float value1, float value2) {
        return value1 > value2;
    }

    public static boolean max(double value1, double value2) {
        return value1 > value2;
    }

    // MAX IN ARRAY function
    public static byte max(byte[] values) {
        return NumberUtils.max(values);
    }

    public static short max(short[] values) {
        return NumberUtils.max(values);
    }

    public static int max(int[] values) {
        return NumberUtils.max(values);
    }

    public static long max(long[] values) {
        return NumberUtils.max(values);
    }

    public static float max(float[] values) {
        return NumberUtils.max(values);
    }

    public static double max(double[] values) {
        return NumberUtils.max(values);
    }

    // MIN function
    public static boolean min(byte value1, byte value2) {
        return value1 < value2;
    }

    public static boolean min(short value1, short value2) {
        return value1 < value2;
    }

    public static boolean min(int value1, int value2) {
        return value1 < value2;
    }

    public static boolean min(long value1, long value2) {
        return value1 < value2;
    }

    public static boolean min(float value1, float value2) {
        return value1 < value2;
    }

    public static boolean min(double value1, double value2) {
        return value1 < value2;
    }

    // MIN IN ARRAY function
    public static byte min(byte[] values) {
        return NumberUtils.min(values);
    }

    public static short min(short[] values) {
        return NumberUtils.min(values);
    }

    public static int min(int[] values) {
        return NumberUtils.min(values);
    }

    public static long min(long[] values) {
        return NumberUtils.min(values);
    }

    public static float min(float[] values) {
        return NumberUtils.min(values);
    }

    public static double min(double[] values) {
        return NumberUtils.min(values);
    }

    // AVERAGE
    public static byte avg(byte[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (byte) (sum(values) / values.length);
        }
        return 0;
    }

    public static short avg(short[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (short) (sum(values) / values.length);
        }
        return 0;
    }

    public static int avg(int[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (int) (sum(values) / values.length);
        }
        return 0;
    }

    public static long avg(long[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (long) (sum(values) / values.length);
        }
        return 0;
    }

    public static float avg(float[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (float) (sum(values) / values.length);
        }
        return 0;
    }

    public static double avg(double[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (double) (sum(values) / values.length);
        }
        return 0;
    }

    // AVERAGE for wrapper types
    public static java.lang.Byte avg(java.lang.Byte[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.lang.Byte.valueOf("0");
        }

        return (byte) (sum(values) / java.lang.Byte.valueOf((byte) valuableSize));
    }

    public static java.lang.Short avg(java.lang.Short[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.lang.Short.valueOf("0");
        }

        return (short) (sum(values) / java.lang.Short.valueOf((short) valuableSize));
    }

    public static java.lang.Integer avg(java.lang.Integer[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.lang.Integer.valueOf("0");
        }

        return (int) (sum(values) / java.lang.Integer.valueOf((int) valuableSize));
    }

    public static java.lang.Long avg(java.lang.Long[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.lang.Long.valueOf("0");
        }

        return (long) (sum(values) / java.lang.Long.valueOf((long) valuableSize));
    }

    public static java.lang.Float avg(java.lang.Float[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.lang.Float.valueOf("0");
        }

        return (float) (sum(values) / java.lang.Float.valueOf((float) valuableSize));
    }

    public static java.lang.Double avg(java.lang.Double[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.lang.Double.valueOf("0");
        }

        return (double) (sum(values) / java.lang.Double.valueOf((double) valuableSize));
    }

    // AVERAGE for big numeric types
    public static java.math.BigInteger avg(java.math.BigInteger[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.math.BigInteger.ZERO;
        }

        return divide(sum(values), java.math.BigInteger.valueOf(valuableSize));
    }

    public static java.math.BigDecimal avg(java.math.BigDecimal[] values) {
        int valuableSize = ArrayTool.getNotNullValuesCount(values);
        if (valuableSize == 0) {
            return java.math.BigDecimal.ZERO;
        }

        return divide(sum(values), java.math.BigDecimal.valueOf(valuableSize));
    }

    // SMALL for primitives
    public static byte small(byte[] values, int position) {
        byte result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static short small(short[] values, int position) {
        short result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static int small(int[] values, int position) {
        int result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static long small(long[] values, int position) {
        long result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static float small(float[] values, int position) {
        float result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static double small(double[] values, int position) {
        double result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    // SMALL for wrapper types
    public static java.lang.Byte small(java.lang.Byte[] values, int position) {
        java.lang.Byte result = java.lang.Byte.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static java.lang.Short small(java.lang.Short[] values, int position) {
        java.lang.Short result = java.lang.Short.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static java.lang.Integer small(java.lang.Integer[] values, int position) {
        java.lang.Integer result = java.lang.Integer.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static java.lang.Long small(java.lang.Long[] values, int position) {
        java.lang.Long result = java.lang.Long.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static java.lang.Float small(java.lang.Float[] values, int position) {
        java.lang.Float result = java.lang.Float.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static java.lang.Double small(java.lang.Double[] values, int position) {
        java.lang.Double result = java.lang.Double.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    // SMALL for big numeric types
    public static java.math.BigInteger small(java.math.BigInteger[] values, int position) {
        java.math.BigInteger result = java.math.BigInteger.ZERO;
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    public static java.math.BigDecimal small(java.math.BigDecimal[] values, int position) {
        java.math.BigDecimal result = java.math.BigDecimal.ZERO;
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[index];

        return result;
    }

    // BIG for primitives
    public static byte big(byte[] values, int position) {
        byte result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static short big(short[] values, int position) {
        short result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static int big(int[] values, int position) {
        int result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static long big(long[] values, int position) {
        long result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static float big(float[] values, int position) {
        float result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static double big(double[] values, int position) {
        double result = 0;
        int index = position - 1; // arrays are 0-based
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    // BIG for wrapper types
    public static java.lang.Byte big(java.lang.Byte[] values, int position) {
        java.lang.Byte result = java.lang.Byte.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static java.lang.Short big(java.lang.Short[] values, int position) {
        java.lang.Short result = java.lang.Short.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static java.lang.Integer big(java.lang.Integer[] values, int position) {
        java.lang.Integer result = java.lang.Integer.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static java.lang.Long big(java.lang.Long[] values, int position) {
        java.lang.Long result = java.lang.Long.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static java.lang.Float big(java.lang.Float[] values, int position) {
        java.lang.Float result = java.lang.Float.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static java.lang.Double big(java.lang.Double[] values, int position) {
        java.lang.Double result = java.lang.Double.valueOf("0");
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    // BIG for big numeric types
    public static java.math.BigInteger big(java.math.BigInteger[] values, int position) {
        java.math.BigInteger result = java.math.BigInteger.ZERO;
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    public static java.math.BigDecimal big(java.math.BigDecimal[] values, int position) {
        java.math.BigDecimal result = java.math.BigDecimal.ZERO;
        int index = position - 1;
        if (values == null) {
            throw new IllegalArgumentException("The array cannot be null");
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        Arrays.sort(values);
        result = values[values.length - 1 - index];

        return result;
    }

    // SUM
    public static byte sum(byte[] values) {
        return (byte) sum(byteArrayToDouble(values));
    }

    public static short sum(short[] values) {
        return (short) sum(shortArrayToDouble(values));
    }

    public static int sum(int[] values) {
        return (int) sum(intArrayToDouble(values));
    }

    public static long sum(long[] values) {
        return (long) sum(longArrayToDouble(values));
    }

    public static float sum(float[] values) {
        return (float) sum(floatArrayToDouble(values));
    }

    // SUM for wrapper types
    public static java.lang.Byte sum(java.lang.Byte[] values) {
        byte sum = java.lang.Byte.valueOf("0");
        for (java.lang.Byte value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum;
    }

    public static java.lang.Short sum(java.lang.Short[] values) {
        short sum = java.lang.Short.valueOf("0");
        for (java.lang.Short value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum;
    }

    public static java.lang.Integer sum(java.lang.Integer[] values) {
        int sum = java.lang.Integer.valueOf("0");
        for (java.lang.Integer value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum;
    }

    public static java.lang.Long sum(java.lang.Long[] values) {
        long sum = java.lang.Long.valueOf("0");
        for (java.lang.Long value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum;
    }

    public static java.lang.Float sum(java.lang.Float[] values) {
        float sum = java.lang.Float.valueOf("0");
        for (java.lang.Float value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum;
    }

    public static java.lang.Double sum(java.lang.Double[] values) {
        double sum = java.lang.Double.valueOf("0");
        for (java.lang.Double value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum;
    }

    // SUM for big numeric types
    public static java.math.BigInteger sum(java.math.BigInteger[] values) {
        java.math.BigInteger sum = java.math.BigInteger.ZERO;
        for (java.math.BigInteger value : values) {
            if (value != null) {
                sum = sum.add(value);
            }
        }
        return sum;
    }

    public static java.math.BigDecimal sum(java.math.BigDecimal[] values) {
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        for (java.math.BigDecimal value : values) {
            if (value != null) {
                sum = sum.add(value);
            }
        }
        return sum;
    }

    // MEDIAN
    public static byte median(byte[] values) {
        double[] doubleArray = byteArrayToDouble(values);
        return (byte) median(doubleArray);
    }

    public static short median(short[] values) {
        double[] doubleArray = shortArrayToDouble(values);
        return (short) median(doubleArray);
    }

    public static int median(int[] values) {
        double[] doubleArray = intArrayToDouble(values);

        return (int) median(doubleArray);
    }

    public static long median(long[] values) {
        double[] doubleArray = longArrayToDouble(values);
        return (long) median(doubleArray);
    }

    public static float median(float[] values) {
        double[] doubleArray = floatArrayToDouble(values);
        return (float) median(doubleArray);
    }

    // MEDIAN for all wrapper types
    public static java.lang.Byte median(java.lang.Byte[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    public static java.lang.Short median(java.lang.Short[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    public static java.lang.Integer median(java.lang.Integer[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    public static java.lang.Long median(java.lang.Long[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    public static java.lang.Float median(java.lang.Float[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    public static java.lang.Double median(java.lang.Double[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    public static java.math.BigInteger median(java.math.BigInteger[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    public static java.math.BigDecimal median(java.math.BigDecimal[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet",
            values.getClass().getName()));
    }

    // PRODUCT
    public static double product(byte[] values) {
        return product(byteArrayToDouble(values));
    }

    public static double product(short[] values) {
        return product(shortArrayToDouble(values));
    }

    public static double product(int[] values) {
        return product(intArrayToDouble(values));
    }

    public static double product(long[] values) {
        return product(longArrayToDouble(values));
    }

    public static double product(float[] values) {
        return product(floatArrayToDouble(values));
    }

    // PRODUCT
    private static double[] byteArrayToDouble(byte[] values) {
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = (double) values[i];
        }
        return doubleArray;
    }

    private static double[] shortArrayToDouble(short[] values) {
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = (double) values[i];
        }
        return doubleArray;
    }

    private static double[] intArrayToDouble(int[] values) {
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = (double) values[i];
        }
        return doubleArray;
    }

    private static double[] longArrayToDouble(long[] values) {
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = (double) values[i];
        }
        return doubleArray;
    }

    private static double[] floatArrayToDouble(float[] values) {
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = (double) values[i];
        }
        return doubleArray;
    }

    // PRODUCT for wrapper types
    public static double product(java.lang.Byte[] values) {
        boolean hasValues = false;

        double res = 1;
        for (java.lang.Byte value : values) {
            if (value != null) {
                res *= value;
                hasValues = true;
            }
        }

        return hasValues ? res : 0;
    }

    public static double product(java.lang.Short[] values) {
        boolean hasValues = false;

        double res = 1;
        for (java.lang.Short value : values) {
            if (value != null) {
                res *= value;
                hasValues = true;
            }
        }

        return hasValues ? res : 0;
    }

    public static double product(java.lang.Integer[] values) {
        boolean hasValues = false;

        double res = 1;
        for (java.lang.Integer value : values) {
            if (value != null) {
                res *= value;
                hasValues = true;
            }
        }

        return hasValues ? res : 0;
    }

    public static double product(java.lang.Long[] values) {
        boolean hasValues = false;

        double res = 1;
        for (java.lang.Long value : values) {
            if (value != null) {
                res *= value;
                hasValues = true;
            }
        }

        return hasValues ? res : 0;
    }

    public static double product(java.lang.Float[] values) {
        boolean hasValues = false;

        double res = 1;
        for (java.lang.Float value : values) {
            if (value != null) {
                res *= value;
                hasValues = true;
            }
        }

        return hasValues ? res : 0;
    }

    public static double product(java.lang.Double[] values) {
        boolean hasValues = false;

        double res = 1;
        for (java.lang.Double value : values) {
            if (value != null) {
                res *= value;
                hasValues = true;
            }
        }

        return hasValues ? res : 0;
    }

    // PRODUCT for big numeric types
    public static java.math.BigInteger product(java.math.BigInteger[] values) {
        boolean hasValues = false;

        java.math.BigInteger res = java.math.BigInteger.ONE;
        for (java.math.BigInteger value : values) {
            if (value != null) {
                res = res.multiply(value);
                hasValues = true;
            }
        }

        return hasValues ? res : java.math.BigInteger.ZERO;
    }

    public static java.math.BigDecimal product(java.math.BigDecimal[] values) {
        boolean hasValues = false;

        java.math.BigDecimal res = java.math.BigDecimal.ONE;
        for (java.math.BigDecimal value : values) {
            if (value != null) {
                res = res.multiply(value);
                hasValues = true;
            }
        }

        return hasValues ? res : java.math.BigDecimal.ZERO;
    }

    // SLICE function
    public static byte[] slice(byte[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static byte[] slice(byte[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static short[] slice(short[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static short[] slice(short[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static int[] slice(int[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static int[] slice(int[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static long[] slice(long[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static long[] slice(long[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static float[] slice(float[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static float[] slice(float[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static double[] slice(double[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static double[] slice(double[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    // SLICE for all wrapper types
    public static java.lang.Byte[] slice(java.lang.Byte[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.lang.Byte[] slice(java.lang.Byte[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (java.lang.Byte[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static java.lang.Short[] slice(java.lang.Short[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.lang.Short[] slice(java.lang.Short[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (java.lang.Short[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static java.lang.Integer[] slice(java.lang.Integer[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.lang.Integer[] slice(java.lang.Integer[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (java.lang.Integer[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static java.lang.Long[] slice(java.lang.Long[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.lang.Long[] slice(java.lang.Long[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (java.lang.Long[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static java.lang.Float[] slice(java.lang.Float[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.lang.Float[] slice(java.lang.Float[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (java.lang.Float[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static java.lang.Double[] slice(java.lang.Double[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.lang.Double[] slice(java.lang.Double[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (java.lang.Double[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static java.math.BigInteger[] slice(java.math.BigInteger[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.math.BigInteger[] slice(java.math.BigInteger[] values,
            int startIndexInclusive,
            int endIndexExclusive) {
        if (values != null) {
            return (java.math.BigInteger[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    public static java.math.BigDecimal[] slice(java.math.BigDecimal[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }

    public static java.math.BigDecimal[] slice(java.math.BigDecimal[] values,
            int startIndexInclusive,
            int endIndexExclusive) {
        if (values != null) {
            return (java.math.BigDecimal[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }

    // MOD is implemented as in Excel.
    public static byte mod(byte number, byte divisor) {
        long quotient = quotient(number, divisor);

        byte intPart = (byte) quotient;
        if (quotient < 0) {
            intPart--;
        }
        return (byte) (number - intPart * divisor);
    }

    public static short mod(short number, short divisor) {
        long quotient = quotient(number, divisor);

        short intPart = (short) quotient;
        if (quotient < 0) {
            intPart--;
        }
        return (short) (number - intPart * divisor);
    }

    public static int mod(int number, int divisor) {
        long quotient = quotient(number, divisor);

        int intPart = (int) quotient;
        if (quotient < 0) {
            intPart--;
        }
        return (int) (number - intPart * divisor);
    }

    public static long mod(long number, long divisor) {
        long quotient = quotient(number, divisor);

        long intPart = (long) quotient;
        if (quotient < 0) {
            intPart--;
        }
        return (long) (number - intPart * divisor);
    }

    public static float mod(float number, float divisor) {
        long quotient = quotient(number, divisor);

        float intPart = (float) quotient;
        if (quotient < 0) {
            intPart--;
        }
        return (float) (number - intPart * divisor);
    }

    public static double mod(double number, double divisor) {
        long quotient = quotient(number, divisor);

        double intPart = (double) quotient;
        if (quotient < 0) {
            intPart--;
        }
        return (double) (number - intPart * divisor);
    }

    // MOD for wrapper types
    public static java.lang.Byte mod(java.lang.Byte number, java.lang.Byte divisor) {
        if (number == null || divisor == null) {
            return java.lang.Byte.valueOf("0");
        }
        return mod((byte) number, (byte) divisor);
    }

    public static java.lang.Short mod(java.lang.Short number, java.lang.Short divisor) {
        if (number == null || divisor == null) {
            return java.lang.Short.valueOf("0");
        }
        return mod((short) number, (short) divisor);
    }

    public static java.lang.Integer mod(java.lang.Integer number, java.lang.Integer divisor) {
        if (number == null || divisor == null) {
            return java.lang.Integer.valueOf("0");
        }
        return mod((int) number, (int) divisor);
    }

    public static java.lang.Long mod(java.lang.Long number, java.lang.Long divisor) {
        if (number == null || divisor == null) {
            return java.lang.Long.valueOf("0");
        }
        return mod((long) number, (long) divisor);
    }

    public static java.lang.Float mod(java.lang.Float number, java.lang.Float divisor) {
        if (number == null || divisor == null) {
            return java.lang.Float.valueOf("0");
        }
        return mod((float) number, (float) divisor);
    }

    public static java.lang.Double mod(java.lang.Double number, java.lang.Double divisor) {
        if (number == null || divisor == null) {
            return java.lang.Double.valueOf("0");
        }
        return mod((double) number, (double) divisor);
    }

    // MOD for big numeric types
    public static java.math.BigInteger mod(java.math.BigInteger number, java.math.BigInteger divisor) {
        if (number == null || divisor == null) {
            return java.math.BigInteger.ZERO;
        }
        long quotient = quotient(number, divisor);

        long intPart = quotient;
        if (quotient < 0) {
            intPart--;
        }
        return number.subtract(java.math.BigInteger.valueOf(intPart).multiply(divisor));
    }

    public static java.math.BigDecimal mod(java.math.BigDecimal number, java.math.BigDecimal divisor) {
        if (number == null || divisor == null) {
            return java.math.BigDecimal.ZERO;
        }
        long quotient = quotient(number, divisor);

        long intPart = quotient;
        if (quotient < 0) {
            intPart--;
        }
        return number.subtract(java.math.BigDecimal.valueOf(intPart).multiply(divisor));
    }

    // QUAOTIENT
    public static long quotient(byte number, byte divisor) {
        return (long) (number / divisor);
    }

    public static long quotient(short number, short divisor) {
        return (long) (number / divisor);
    }

    public static long quotient(int number, int divisor) {
        return (long) (number / divisor);
    }

    public static long quotient(long number, long divisor) {
        return (long) (number / divisor);
    }

    public static long quotient(float number, float divisor) {
        return (long) (number / divisor);
    }

    public static long quotient(double number, double divisor) {
        return (long) (number / divisor);
    }

    // QUAOTIENT for wrapper types
    public static long quotient(java.lang.Byte number, java.lang.Byte divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((byte) number, (byte) divisor);
    }

    public static long quotient(java.lang.Short number, java.lang.Short divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((short) number, (short) divisor);
    }

    public static long quotient(java.lang.Integer number, java.lang.Integer divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((int) number, (int) divisor);
    }

    public static long quotient(java.lang.Long number, java.lang.Long divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((long) number, (long) divisor);
    }

    public static long quotient(java.lang.Float number, java.lang.Float divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((float) number, (float) divisor);
    }

    public static long quotient(java.lang.Double number, java.lang.Double divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((double) number, (double) divisor);
    }

    // QUAOTIENT for big numeric types
    public static long quotient(java.math.BigInteger number, java.math.BigInteger divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return divide(number, divisor).longValue();
    }

    public static long quotient(java.math.BigDecimal number, java.math.BigDecimal divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return divide(number, divisor).longValue();
    }

    // SORT
    public static byte[] sort(byte[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static short[] sort(short[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static int[] sort(int[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static long[] sort(long[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static float[] sort(float[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static double[] sort(double[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }
    

    // SORT for all wrapper types
    public static java.lang.Byte[] sort(java.lang.Byte[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    public static java.lang.Short[] sort(java.lang.Short[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    public static java.lang.Integer[] sort(java.lang.Integer[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    public static java.lang.Long[] sort(java.lang.Long[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    public static java.lang.Float[] sort(java.lang.Float[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    public static java.lang.Double[] sort(java.lang.Double[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    public static java.math.BigInteger[] sort(java.math.BigInteger[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    public static java.math.BigDecimal[] sort(java.math.BigDecimal[] values) {
        if (values != null) {
            values = ArrayTool.removeNulls(values);
            Arrays.sort(values);
        }
        return values;
    }

    // <<< END INSERT Functions >>>

    public static boolean eq(float x, float y) {
        if (Float.compare(x, y) == 0)
            return true;
        if (Float.isInfinite(x) || Float.isInfinite(y) || Float.isNaN(x) || Float.isNaN(y))
            return false;
        return Math.abs(x - y) <= Math.ulp(x);
    }

    public static boolean ne(float x, float y) {
        return !eq(x, y);
    }

    public static boolean gt(float x, float y) {
        if (Float.POSITIVE_INFINITY == x && Float.POSITIVE_INFINITY != y && !Float.isNaN(y))
            return true;
        return Math.abs(x - y) > Math.ulp(x) && x > y;
    }

    public static boolean ge(float x, float y) {
        return eq(x, y) || gt(x, y);
    }

    public static boolean lt(float x, float y) {
        if (Float.NEGATIVE_INFINITY == x && Float.NEGATIVE_INFINITY != y && !Float.isNaN(y))
            return true;
        return Math.abs(x - y) > Math.ulp(x) && x < y;
    }

    public static boolean le(float x, float y) {
        return eq(x, y) || lt(x, y);
    }

    public static boolean eq(double x, double y) {
        if (Double.compare(x, y) == 0)
            return true;
        if (Double.isInfinite(x) || Double.isInfinite(y) || Double.isNaN(x) || Double.isNaN(y))
            return false;
        return Math.abs(x - y) <= Math.ulp(x);
    }

    public static boolean eq(double x, double y, double delta) {
        if (Double.compare(x, y) == 0)
            return true;
        if (Double.isInfinite(x) || Double.isInfinite(y) || Double.isNaN(x) || Double.isNaN(y))
            return false;
        return Math.abs(x - y) <= delta;
    }

    public static boolean ne(double x, double y) {
        return !eq(x, y);
    }

    public static boolean gt(double x, double y) {
        if (Double.POSITIVE_INFINITY == x && Double.POSITIVE_INFINITY != y && !Double.isNaN(y))
            return true;
        return Math.abs(x - y) > Math.ulp(x) && x > y;
    }

    public static boolean ge(double x, double y) {
        return eq(x, y) || gt(x, y);
    }

    public static boolean lt(double x, double y) {
        if (Double.NEGATIVE_INFINITY == x && Double.NEGATIVE_INFINITY != y && !Double.isNaN(y))
            return true;
        return Math.abs(x - y) > Math.ulp(x) && x < y;
    }

    public static boolean le(double x, double y) {
        return eq(x, y) || lt(x, y);
    }

    public static boolean eq(BigDecimal x, BigDecimal y) {
        return x.subtract(y).abs().compareTo(x.ulp()) <= 0;
    }

    /**
     * Divide one BigDecimal to another. When providing a result of divide
     * operation, the precision '5' and {@link RoundingMode.HALF_UP} settings
     * are used.
     * 
     * @param values
     * @return rounded to 5 values after comma and {@link RoundingMode.HALF_UP}
     *         value.
     */
    public static BigDecimal divide(BigDecimal number, BigDecimal divisor) {
        if (number == null || divisor == null) {
            return null;
        }
        return number.divide(divisor, MathContext.DECIMAL128);
    }

    public static BigInteger divide(BigInteger number, BigInteger divisor) {
        if (number == null || divisor == null) {
            return null;
        }
        return number.divide(divisor);
    }

    // public static BigInteger divide(Number number, Number divisor) {
    // if (number == null || divisor == null) {
    // return null;
    // }
    // return number / divisor;
    // }

    // MAX for Big types
    public static boolean max(BigInteger value1, BigInteger value2) {
        return value1.compareTo(value2) > 0;
    }

    public static boolean max(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) > 0;
    }

    // MAX IN ARRAY
    public static char max(char[] values) {
        char max = Character.MIN_VALUE;
        for (char value : values) {
            if (value > max)
                max = value;
        }
        return max;
    }

    public static BigInteger max(BigInteger[] values) {
        return (BigInteger) max((Object[]) values);
    }

    public static BigDecimal max(BigDecimal[] values) {
        return (BigDecimal) max((Object[]) values);
    }

    @SuppressWarnings("unchecked")
    public static Object max(Object[] values) {
        if (values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }
        values = ArrayTool.removeNulls(values);
        if (values.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
        if (!(ClassUtils.isAssignable(values.getClass().getComponentType(), Number.class) && ClassUtils.isAssignable(values.getClass()
            .getComponentType(),
            Comparable.class))) {
            throw new IllegalArgumentException("Income array must be comparable numeric.");
        }
        Comparable<Number>[] numberArray = (Comparable<Number>[]) values;
        Number max = (Number) numberArray[0];
        for (int i = 1; i < numberArray.length; i++) {
            if (numberArray[i].compareTo(max) > 0) {
                max = (Number) numberArray[i];
            }
        }
        return max;
    }

    // MIN for big types
    public static boolean min(BigInteger value1, BigInteger value2) {
        return value1.compareTo(value2) < 0;
    }

    public static boolean min(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) < 0;
    }

    // MIN IN ARRAY
    public static char min(char[] values) {
        char min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    public static BigInteger min(BigInteger[] values) {
        return (BigInteger) min((Object[]) values);
    }

    public static BigDecimal min(BigDecimal[] values) {
        return (BigDecimal) min((Object[]) values);
    }

    @SuppressWarnings("unchecked")
    public static Object min(Object[] values) {
        if (values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }
        values = ArrayTool.removeNulls(values);
        if (values.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
        if (!(ClassUtils.isAssignable(values.getClass().getComponentType(), Number.class) && ClassUtils.isAssignable(values.getClass()
            .getComponentType(),
            Comparable.class))) {
            throw new IllegalArgumentException("Income array must be comparable numeric.");
        }
        Comparable<Number>[] numberArray = (Comparable<Number>[]) values;
        Number min = (Number) numberArray[0];
        for (int i = 1; i < numberArray.length; i++) {
            if (numberArray[i].compareTo(min) < 0) {
                min = (Number) numberArray[i];
            }
        }
        return min;
    }

    // SUMMARY
    public static double sum(double[] values) {
        if (values == null) throw new IllegalArgumentException("The Array must not be null");
        if (values.length == 0) {
            return Double.NaN;
        }
        double res = 0.0;
        for (double val : values) {
            res += val;
        }
        return res;
    }

    // MEDIAN
    public static double median(double[] values) {
        if (values == null) throw new IllegalArgumentException("The Array must not be null");
        int length = values.length;
        if (length == 0) {
            return Double.NaN;
        } else if (length == 1) {
            return values[0];
        } else if (length == 2) {
            return (values[0] + values[1]) * 0.5;
        }
        double[] copy = Arrays.copyOf(values, length);
        Arrays.sort(copy);
        length--;
        int index = length >> 1;
        if (length % 2 == 0) {
            return copy[index];
        } else {
            return (copy[index] + copy[index + 1]) * 0.5;
        }
    }
    // PRODUCT
    public static double product(double[] values) {
        if (values == null) throw new IllegalArgumentException("The Array must not be null");
        if (values.length == 0) {
            return Double.NaN;
        }
        double res = 1.0;
        for (double val : values) {
            res *= val;
        }
        return res;
    }

    public static double round(double x, int scale, int roundingMethod) {
        try {
            return (new BigDecimal
                    (Double.toString(x))
                    .setScale(scale, roundingMethod))
                    .doubleValue();
        } catch (NumberFormatException ex) {
            if (Double.isInfinite(x)) {
                return x;
            } else {
                return Double.NaN;
            }
        }
    }

    public static float round(float x, int scale, int roundingMethod) {
        try {
            return (new BigDecimal
                    (Float.toString(x))
                    .setScale(scale, roundingMethod))
                    .floatValue();
        } catch (NumberFormatException ex) {
            if (Float.isInfinite(x)) {
                return x;
            } else {
                return Float.NaN;
            }
        }
    }
}
