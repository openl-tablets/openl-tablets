package org.openl.util.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.openl.util.ArrayTool;

/**
 * @author DLiauchuk
 */
public class MathUtils {

    private static <T extends Number> double[] numberArrayToDoubleArray(T[] values) {
        if (values == null) {
            return null;
        }
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                doubleArray[i] = values[i].doubleValue();
            }
        }
        return doubleArray;
    }

    private static double[] byteArrayToDoubleArray(byte[] values) {
        if (values == null) {
            return null;
        }
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = values[i];
        }
        return doubleArray;
    }

    private static double[] shortArrayToDoubleArray(short[] values) {
        if (values == null) {
            return null;
        }
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = values[i];
        }
        return doubleArray;
    }

    private static double[] intArrayToDoubleArray(int[] values) {
        if (values == null) {
            return null;
        }
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = values[i];
        }
        return doubleArray;
    }

    private static double[] longArrayToDoubleArray(long[] values) {
        if (values == null) {
            return null;
        }
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = values[i];
        }
        return doubleArray;
    }

    private static double[] floatArrayToDoubleArray(float[] values) {
        if (values == null) {
            return null;
        }
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            doubleArray[i] = values[i];
        }
        return doubleArray;
    }

    // SMALL
    public static <T extends Comparable<T>> T small(T[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        values = ArrayTool.removeNulls(values);
        validateIndex(index < 0 || values.length <= index, position);
        T[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Byte small(byte[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        byte[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Short small(short[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        short[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Integer small(int[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        int[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Long small(long[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        long[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Float small(float[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        float[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Double small(double[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        double[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    // BIG
    public static <T extends Comparable<T>> T big(T[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        values = ArrayTool.removeNulls(values);
        validateIndex(index < 0 || values.length <= index, position);
        T[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Byte big(byte[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        byte[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Short big(short[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        short[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Integer big(int[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        int[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Long big(long[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        long[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Float big(float[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        float[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Double big(double[] values, int position) {
        if (values == null) {
            return null;
        }
        int index = position - 1;
        validateIndex(index < 0 || values.length <= index, position);
        double[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    private static void validateIndex(boolean throwException, int position) {
        if (throwException) {
            throw new IllegalArgumentException(
                    "There is no position '%d' in the given array.".formatted(position));
        }
    }

    // SUM
    public static Byte sum(byte[] values) {
        if (values == null) {
            return null;
        }
        byte sum = 0;
        for (byte a : values) {
            sum = (byte) (sum + a);
        }
        return sum;
    }

    public static Short sum(short[] values) {
        if (values == null) {
            return null;
        }
        short sum = 0;
        for (short a : values) {
            sum = (short) (sum + a);
        }
        return sum;
    }

    public static Integer sum(int[] values) {
        if (values == null) {
            return null;
        }
        int sum = 0;
        for (int a : values) {
            sum = sum + a;
        }
        return sum;
    }

    public static Long sum(long[] values) {
        if (values == null) {
            return null;
        }
        long sum = 0;
        for (long a : values) {
            sum = sum + a;
        }
        return sum;
    }

    public static Float sum(float[] values) {
        if (values == null) {
            return null;
        }
        float sum = 0;
        for (float a : values) {
            sum = sum + a;
        }
        return sum;
    }

    public static Double sum(double[] values) {
        if (values == null) {
            return null;
        }
        double sum = 0;
        for (double a : values) {
            sum = sum + a;
        }
        return sum;
    }

    // MEDIAN
    public static <T extends Number> Double median(T[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        if (values.length == 0) {
            return null;
        }
        double[] doubleArray = numberArrayToDoubleArray(values);
        return median(doubleArray);
    }

    public static Float median(Float[] values) {
        Double median = median((Number[]) values);
        return median == null ? null : median.floatValue();
    }

    public static BigDecimal median(BigInteger[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        int length = values.length;
        if (length == 0) {
            return null;
        } else if (length == 1) {
            return new BigDecimal(values[0]);
        } else if (length == 2) {
            BigDecimal v = new BigDecimal(values[0].add(values[1]));
            return v.divide(BigDecimal.valueOf(2));
        }
        BigInteger[] copy = Arrays.copyOf(values, length);
        Arrays.sort(copy);
        length--;
        int index = length >> 1;
        if (length % 2 == 0) {
            return new BigDecimal(copy[index]);
        } else {
            BigDecimal v = new BigDecimal(copy[index].add(copy[index + 1]));
            return v.divide(BigDecimal.valueOf(2));
        }
    }

    public static BigDecimal median(BigDecimal[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        int length = values.length;
        if (length == 0) {
            return null;
        } else if (length == 1) {
            return values[0];
        } else if (length == 2) {
            BigDecimal v = values[0].add(values[1]);
            return new BigDecimal("0.5").multiply(v);
        }
        BigDecimal[] copy = Arrays.copyOf(values, length);
        Arrays.sort(copy);
        length--;
        int index = length >> 1;
        if (length % 2 == 0) {
            return copy[index];
        } else {
            BigDecimal v = copy[index].add(copy[index + 1]);
            return new BigDecimal("0.5").multiply(v);
        }
    }

    public static Double median(double[] values) {
        if (values == null) {
            return null;
        }
        int length = values.length;
        if (length == 0) {
            return null;
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

    public static Double median(byte[] values) {
        return median(byteArrayToDoubleArray(values));
    }

    public static Double median(short[] values) {
        return median(shortArrayToDoubleArray(values));
    }

    public static Double median(int[] values) {
        return median(intArrayToDoubleArray(values));
    }

    public static Double median(long[] values) {
        return median(longArrayToDoubleArray(values));
    }

    public static Float median(float[] values) {
        Double median = median(floatArrayToDoubleArray(values));
        return median == null ? null : median.floatValue();
    }

}
