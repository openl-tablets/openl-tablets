package org.openl.util.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

import org.openl.util.ArrayTool;

/**
 *
 * @author DLiauchuk
 * 
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

    // AVERAGE
    public static <T extends Number> Double avg(T[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        if (values.length == 0) {
            return null;
        }

        double[] doubleValues = numberArrayToDoubleArray(values);
        return sum(doubleValues) / values.length;
    }

    public static Float avg(Float[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        if (values.length == 0) {
            return null;
        }

        return sum(values, new Adder<Float>() {
            @Override
            public Float add(Float a, Float b) {
                return a + b;
            }

            @Override
            public Float zero() {
                return 0f;
            }
        }) / values.length;
    }

    public static BigDecimal avg(java.math.BigInteger[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        if (values.length == 0) {
            return null;
        }

        return divide(new BigDecimal(sum(values, new Adder<BigInteger>() {
            @Override
            public BigInteger add(BigInteger a, BigInteger b) {
                return a.add(b);
            }

            @Override
            public BigInteger zero() {
                return BigInteger.ZERO;
            }
        })), java.math.BigDecimal.valueOf(values.length));
    }

    public static BigDecimal avg(java.math.BigDecimal[] values) {
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        if (values.length == 0) {
            return null;
        }

        return divide(sum(values, new Adder<BigDecimal>() {
            @Override
            public BigDecimal add(BigDecimal a, BigDecimal b) {
                return a.add(b);
            }

            @Override
            public BigDecimal zero() {
                return BigDecimal.ZERO;
            }
        }), java.math.BigDecimal.valueOf(values.length));
    }

    public static Double avg(byte[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double sum = 0d;
        for (byte a : values) {
            sum = sum + a;
        }
        return sum / values.length;
    }

    public static Double avg(short[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double sum = 0d;
        for (short a : values) {
            sum = sum + a;
        }
        return sum / values.length;
    }

    public static Double avg(int[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double sum = 0d;
        for (int a : values) {
            sum = sum + a;
        }
        return sum / values.length;
    }

    public static Double avg(long[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double sum = 0d;
        for (long a : values) {
            sum = sum + a;
        }
        return sum / values.length;
    }

    public static Float avg(float[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        float sum = 0f;
        for (float a : values) {
            sum = sum + a;
        }
        return sum / values.length;
    }

    public static Double avg(double[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        double sum = 0d;
        for (double a : values) {
            sum = sum + a;
        }
        return sum / values.length;
    }

    // SMALL
    public static <T extends Comparable<T>> T small(T[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        T[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Byte small(byte[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        byte[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Short small(short[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        short[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Integer small(int[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        int[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Long small(long[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        long[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Float small(float[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        float[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    public static Double small(double[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        double[] v = values.clone();
        Arrays.sort(v);
        return v[index];
    }

    // BIG
    public static <T extends Comparable<T>> T big(T[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        values = ArrayTool.removeNulls(values);
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        T[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Byte big(byte[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        byte[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Short big(short[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        short[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Integer big(int[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        int[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Long big(long[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        long[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Float big(float[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        float[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    public static Double big(double[] values, int position) {
        int index = position - 1;
        if (values == null) {
            return null;
        }
        if (index < 0 || values.length <= index) {
            throw new IllegalArgumentException(String.format("There is no position '%d' in the given array", position));
        }
        double[] v = values.clone();
        Arrays.sort(v);
        return v[v.length - 1 - index];
    }

    // SUM
    private static interface Adder<T extends Number> {
        T add(T a, T b);

        T zero();
    }

    private static <T extends Number> T sum(T[] values, Adder<T> adder) {
        if (values == null) {
            return null;
        }
        T sum = adder.zero();
        boolean hasValues = false;
        for (T value : values) {
            if (value != null) {
                sum = adder.add(sum, value);
                hasValues = true;
            }
        }
        return hasValues ? sum : null;
    }

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
            sum = sum + (long) a;
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

    // PRODUCT
    private static interface Multiplicator<T extends Number, R extends Number> {
        R multiply(R a, T b);

        R one();

        R zero();
    }

    private static <T extends Number, R extends Number> R product(T[] values, Multiplicator<T, R> multiplicator) {
        if (values == null) {
            return null;
        }
        boolean hasValues = false;
        R res = multiplicator.one();
        for (T value : values) {
            if (value != null) {
                res = multiplicator.multiply(res, value);
                hasValues = true;
            }
        }
        return hasValues ? res : null;

    }

    public static Long product(Byte[] values) {
        return product(values, new Multiplicator<Byte, Long>() {
            @Override
            public Long multiply(Long a, Byte b) {
                return a * b.longValue();
            }

            @Override
            public Long one() {
                return 1l;
            }

            @Override
            public Long zero() {
                return 0l;
            }
        });
    }

    public static Long product(Short[] values) {
        return product(values, new Multiplicator<Short, Long>() {
            @Override
            public Long multiply(Long a, Short b) {
                return a * b.longValue();
            }

            @Override
            public Long one() {
                return 1l;
            }

            @Override
            public Long zero() {
                return 0l;
            }
        });
    }

    public static Long product(Integer[] values) {
        return product(values, new Multiplicator<Integer, Long>() {
            @Override
            public Long multiply(Long a, Integer b) {
                return a * b.longValue();
            }

            @Override
            public Long one() {
                return 1l;
            }

            @Override
            public Long zero() {
                return 0l;
            }
        });
    }

    public static Long product(Long[] values) {
        return product(values, new Multiplicator<Long, Long>() {
            @Override
            public Long multiply(Long a, Long b) {
                return a * b;
            }

            @Override
            public Long one() {
                return 1l;
            }

            @Override
            public Long zero() {
                return 0l;
            }
        });
    }

    public static Float product(Float[] values) {
        return product(values, new Multiplicator<Float, Float>() {
            @Override
            public Float multiply(Float a, Float b) {
                return a * b;
            }

            @Override
            public Float one() {
                return 1f;
            }

            @Override
            public Float zero() {
                return 0f;
            }
        });
    }

    public static Double product(Double[] values) {
        return product(values, new Multiplicator<Double, Double>() {
            @Override
            public Double multiply(Double a, Double b) {
                return a * b;
            }

            @Override
            public Double one() {
                return 1d;
            }

            @Override
            public Double zero() {
                return 0d;
            }
        });
    }

    public static java.math.BigInteger product(java.math.BigInteger[] values) {
        return product(values, new Multiplicator<BigInteger, BigInteger>() {
            @Override
            public BigInteger multiply(BigInteger a, BigInteger b) {
                return a.multiply(b);
            }

            @Override
            public BigInteger one() {
                return BigInteger.ONE;
            }

            @Override
            public BigInteger zero() {
                return BigInteger.ZERO;
            }
        });
    }

    public static java.math.BigDecimal product(java.math.BigDecimal[] values) {
        return product(values, new Multiplicator<BigDecimal, BigDecimal>() {
            @Override
            public BigDecimal multiply(BigDecimal a, BigDecimal b) {
                return a.multiply(b);
            }

            @Override
            public BigDecimal one() {
                return BigDecimal.ONE;
            }

            @Override
            public BigDecimal zero() {
                return BigDecimal.ZERO;
            }
        });
    }

    public static Long product(byte[] values) {
        if (values == null) {
            return null;
        }
        long res = 1;
        for (byte a : values) {
            res = res * (long) a;
        }
        return values.length > 0 ? res : 0l;
    }

    public static Long product(short[] values) {
        if (values == null) {
            return null;
        }
        long res = 1;
        for (short a : values) {
            res = res * (long) a;
        }
        return values.length > 0 ? res : 0l;
    }

    public static Long product(int[] values) {
        if (values == null) {
            return null;
        }
        long res = 1;
        for (int a : values) {
            res = res * (long) a;
        }
        return values.length > 0 ? res : 0l;
    }

    public static Long product(long[] values) {
        if (values == null) {
            return null;
        }
        long res = 1;
        for (long a : values) {
            res = res * a;
        }
        return values.length > 0 ? res : 0l;
    }

    public static Float product(float[] values) {
        if (values == null) {
            return null;
        }
        float res = 1;
        for (float a : values) {
            res = res * a;
        }
        return values.length > 0 ? res : 0l;
    }

    public static Double product(double[] values) {
        if (values == null) {
            return null;
        }
        double res = 1;
        for (double a : values) {
            res = res * a;
        }
        return values.length > 0 ? res : 0l;
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

    public static java.math.BigDecimal median(java.math.BigInteger[] values) {
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

    public static java.math.BigDecimal median(java.math.BigDecimal[] values) {
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
            return (new BigDecimal("0.5")).multiply(v);
        }
        BigDecimal[] copy = Arrays.copyOf(values, length);
        Arrays.sort(copy);
        length--;
        int index = length >> 1;
        if (length % 2 == 0) {
            return copy[index];
        } else {
            BigDecimal v = copy[index].add(copy[index + 1]);
            return (new BigDecimal("0.5")).multiply(v);
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
    public static Byte mod(Byte number, Byte divisor) {
        if (number == null || divisor == null) {
            return Byte.valueOf("0");
        }
        return mod((byte) number, (byte) divisor);
    }

    public static Short mod(Short number, Short divisor) {
        if (number == null || divisor == null) {
            return Short.valueOf("0");
        }
        return mod((short) number, (short) divisor);
    }

    public static Integer mod(Integer number, Integer divisor) {
        if (number == null || divisor == null) {
            return Integer.valueOf("0");
        }
        return mod((int) number, (int) divisor);
    }

    public static Long mod(Long number, Long divisor) {
        if (number == null || divisor == null) {
            return Long.valueOf("0");
        }
        return mod((long) number, (long) divisor);
    }

    public static Float mod(Float number, Float divisor) {
        if (number == null || divisor == null) {
            return Float.valueOf("0");
        }
        return mod((float) number, (float) divisor);
    }

    public static Double mod(Double number, Double divisor) {
        if (number == null || divisor == null) {
            return Double.valueOf("0");
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
    public static long quotient(Byte number, Byte divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((byte) number, (byte) divisor);
    }

    public static long quotient(Short number, Short divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((short) number, (short) divisor);
    }

    public static long quotient(Integer number, Integer divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((int) number, (int) divisor);
    }

    public static long quotient(Long number, Long divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((long) number, (long) divisor);
    }

    public static long quotient(Float number, Float divisor) {
        if (number == null || divisor == null) {
            return 0;
        }
        return quotient((float) number, (float) divisor);
    }

    public static long quotient(Double number, Double divisor) {
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
    public static <T extends Comparable<?>> T[] sort(T[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

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

    /**
     * Divide one BigDecimal to another. When providing a result of divide
     * operation, the precision '5' and {@link RoundingMode#HALF_UP} settings
     * are used.
     * 
     * @return rounded to 5 values after comma and {@link RoundingMode#HALF_UP}
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

}
