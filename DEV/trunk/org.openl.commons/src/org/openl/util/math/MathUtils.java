package org.openl.util.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.rank.Median;
import org.openl.util.ArrayTool;

public class MathUtils {

	public static boolean eq(float x, float y) {
		return Math.abs(x - y) <= Math.ulp(x);
	}

	public static boolean ne(float x, float y) {
		return !eq(x, y);
	}

	public static boolean gt(float x, float y) {
		return Math.abs(x - y) > Math.ulp(x) && x > y;
	}

	public static boolean ge(float x, float y) {
		return eq(x, y) || gt(x, y);
	}

	public static boolean lt(float x, float y) {
		return Math.abs(x - y) > Math.ulp(x) && x < y;
	}

	public static boolean le(float x, float y) {
		return eq(x, y) || lt(x, y);
	}

	public static boolean eq(double x, double y) {
		return Math.abs(x - y) <= Math.ulp(x);
	}

	public static boolean ne(double x, double y) {
		return !eq(x, y);
	}

	public static boolean gt(double x, double y) {
		return Math.abs(x - y) > Math.ulp(x) && x > y;
	}
	
	public static boolean ge(double x, double y) {
		return eq(x, y) || gt(x, y);
	}
	
	public static boolean lt(double x, double y) {
		return Math.abs(x - y) > Math.ulp(x) && x < y;
	}

	public static boolean le(double x, double y) {
		return eq(x, y) || lt(x, y);
	}
	
	public static boolean eq(BigDecimal x, BigDecimal y) {
        return x.subtract(y).abs().compareTo(x.ulp()) <= 0;
    }
	
	 /**
     * Divide one BigDecimal to another.
     * When providing a result of divide operation, the precision '5' and {@link RoundingMode.HALF_UP}
     * settings are used.
     * 
     * @param values
     * @return rounded to 5 values after comma and {@link RoundingMode.HALF_UP} value.
     */
	public static BigDecimal divide(BigDecimal number, BigDecimal divisor) {
	    if (number == null || divisor == null) {
	        return null;
	    }
	    return number.divide(divisor, 5, RoundingMode.HALF_UP);
	}
	
	// MAX	
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
	
	public static boolean max(BigInteger value1, BigInteger value2) {
        return value1.compareTo(value2) > 0;
    }
	
	public static boolean max(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) > 0;
    }
	
	// MAX IN ARRAY
    public static byte max(byte[] values) {
        return NumberUtils.max(values);
    }
    
    public static char max(char[] values) {
        char max = Character.MIN_VALUE;
        for (char value : values) {
            if (value > max)
                max = value;
        }
        return max;
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
    
    public static BigInteger max(BigInteger[] values) {
        return (BigInteger) max((Object[])values);
    }
    
    public static BigDecimal max(BigDecimal[] values) {
        return (BigDecimal) max((Object[])values);
    }
    
    @SuppressWarnings("unchecked")
    public static Object max(Object[] values) {
        if (values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }            
        if (values.length == 0) {
            throw  new IllegalArgumentException("Array cannot be empty.");
        }
        if (!(ClassUtils.isAssignable(values.getClass().getComponentType(), Number.class, true) && 
                ClassUtils.isAssignable(values.getClass().getComponentType(), Comparable.class, true))) {
            throw new IllegalArgumentException("Income array must be comparable numeric.");
        }
        Comparable<Number>[] numberArray = (Comparable<Number>[])values;
        Number max = (Number) numberArray[0];
        for (int i = 1; i < numberArray.length; i++) {
            if (numberArray[i].compareTo(max) > 0) {
                max = (Number) numberArray[i];
            }
        }        
        return max;
    }
    
    // MIN
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
    
    public static boolean min(BigInteger value1, BigInteger value2) {
        return value1.compareTo(value2) < 0;
    }
    
    public static boolean min(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) < 0;
    }
    
    //MIN IN ARRAY   
    public static byte min(byte[] values) {
        return NumberUtils.min(values);
    }
    
    public static char min(char[] values) {
        char min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
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
    
    public static BigInteger min(BigInteger[] values) {
        return (BigInteger) min((Object[])values);
    }
    
    public static BigDecimal min(BigDecimal[] values) {
        return (BigDecimal) min((Object[])values);
    }
    
    @SuppressWarnings("unchecked")
    public static Object min(Object[] values) {
        if (values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }            
        if (values.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }
        if (!(ClassUtils.isAssignable(values.getClass().getComponentType(), Number.class, true) && 
                ClassUtils.isAssignable(values.getClass().getComponentType(), Comparable.class, true))) {
            throw new IllegalArgumentException("Income array must be comparable numeric.");
        }
        Comparable<Number>[] numberArray = (Comparable<Number>[])values;
        Number min = (Number) numberArray[0];
        for (int i = 1; i < numberArray.length; i++) {
            if (numberArray[i].compareTo(min) < 0) {
                min = (Number) numberArray[i];
            }
        }        
        return min;
    }
    
    //AVERAGE
    public static byte avg(byte[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (byte) (sum(values)/values.length);
        }
        return 0;
    }
    
    public static short avg(short[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (short) (sum(values)/values.length);
        }
        return 0;
    }
    
    public static int avg(int[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (int) (sum(values)/values.length);
        }
        return 0;
    }
    
    public static long avg(long[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (long) (sum(values)/values.length);
        }
        return 0;
    }
    
    public static float avg(float[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (float) (sum(values)/values.length);
        }
        return 0;
    }
    
    public static double avg(double[] values) {
        if (!ArrayUtils.isEmpty(values)) {
            return sum(values)/values.length;
        }
        return 0;
    }
    
    public static BigInteger avg(BigInteger[] values) {
        if (ArrayTool.noNulls(values)) {
            return sum(values).divide(BigInteger.valueOf(values.length));
        }
        return BigInteger.valueOf(0);
    }
    
    /**
     * Returns the average value for the income array of elements.
     * When providing a result of divide operation, the precision '5' and {@link RoundingMode.HALF_UP}
     * settings are used.
     * 
     * @param values
     * @return rounded to 5 values after comma and RoundingMode.HALF_UP value.
     */
    public static BigDecimal avg(BigDecimal[] values) {
        if (ArrayTool.noNulls(values)) {
            return divide(sum(values), BigDecimal.valueOf(values.length));
        }
        return BigDecimal.valueOf(0);
    }
    
    // SUMMARY
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
    
    public static double sum(double[] values) {
        // used commons function
        return StatUtils.sum(values);        
    }
    
    public static BigInteger sum(BigInteger[] values) {
        BigInteger sum = BigInteger.valueOf(0);
        if (ArrayTool.noNulls(values)) {
            for (BigInteger value : values) {
                sum = sum.add(value);
            }            
        }
        return sum;
    }
    
    public static BigDecimal sum(BigDecimal[] values) {
        BigDecimal sum = BigDecimal.valueOf(0);
        if (ArrayTool.noNulls(values)) {
            for (BigDecimal value : values) {
                sum = sum.add(value);
            }            
        }
        return sum;
    }
    
    // MEDIAN
    public static byte median(byte[] values) {
        double[] doubleArray = byteArrayToDouble(values);
        
        Median median = new Median();
        return (byte) median.evaluate(doubleArray, 0, doubleArray.length);
    }
    
    public static short median(short[] values) {
        double[] doubleArray = shortArrayToDouble(values);
        
        Median median = new Median();        
        return (short) median.evaluate(doubleArray, 0, doubleArray.length);
    }    
    
    public static int median(int[] values) {
        double[] doubleArray = intArrayToDouble(values);
        
        Median median = new Median();
        return (int) median.evaluate(doubleArray, 0, doubleArray.length);
    }    
    
    public static long median(long[] values) {
        double[] doubleArray = longArrayToDouble(values);
        
        Median median = new Median();
        return (long) median.evaluate(doubleArray, 0, doubleArray.length);
    }    
    
    public static float median(float[] values) {
        double[] doubleArray = floatArrayToDouble(values);
        
        Median median = new Median();
        return (float) median.evaluate(doubleArray, 0, doubleArray.length);
    }    
    
    public static double median(double[] values) {        
        Median median = new Median();
        return median.evaluate(values, 0, values.length);
    }
    
    public static BigInteger median(BigInteger[] values) {    
        // TODO implement
        throw new NotImplementedException("Method median for BigInteger is not implemented yet");
    }
    
    public static BigDecimal median(BigDecimal[] values) {
        // TODO implement
        throw new NotImplementedException("Method median for BigDecimal is not implemented yet");
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
    
    public static double product(double[] values) {
        // used commons function
        return StatUtils.product(values);
    }
    
    public static BigInteger product(BigInteger[] values) {
        BigInteger res = BigInteger.valueOf(0);
        if (ArrayTool.noNulls(values)) {
            res = BigInteger.valueOf(1);
            for (BigInteger value : values) {
                res = res.multiply(value);
            }
        }
        return res;
    }
    
    public static BigDecimal product(BigDecimal[] values) {
        BigDecimal res = BigDecimal.valueOf(0);
        if (ArrayTool.noNulls(values)) {
            res = BigDecimal.valueOf(1);
            for (BigDecimal value : values) {
                res = res.multiply(value);
            }
        }
        return res;
    }
    
    // QUAOTIENT
    public static byte quaotient(byte number, byte divisor) {
        return (byte) (number/divisor);
    }
    
    public static short quaotient(short number, short divisor) {
        return (short) (number/divisor);
    }
    
    public static int quaotient(int number, int divisor) {
        return number/divisor;
    }
    
    public static long quaotient(long number, long divisor) {
        return number/divisor;
    }
    
    public static long quaotient(float number, float divisor) {
        return (long) (number/divisor);
    }
    
    public static long quaotient(double number, double divisor) {
        return (long) (number/divisor);
    }
    
    public static long quaotient(BigInteger number, BigInteger divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return number.divide(divisor).longValue();
    }
    
    public static long quaotient(BigDecimal number, BigDecimal divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return divide(number, divisor).longValue();
    }
    
    // MOD is implemented as in Excel.
    public static byte mod(byte number, byte divisor) {        
        byte quaotient = quaotient(number, divisor);
        
        byte intPart = quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return (byte) (number - intPart * divisor);
    }
    
    public static short mod(short number, short divisor) {        
        short quaotient = quaotient(number, divisor);
        
        short intPart = quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return (short) (number - intPart * divisor);
    }
    
    public static int mod(int number, int divisor) {        
        int quaotient = quaotient(number, divisor);
        
        int intPart = quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return number - intPart * divisor;
    }
    
    public static long mod(long number, long divisor) {        
        long quaotient = quaotient(number, divisor);
        
        long intPart = quaotient;
        if (quaotient < 0) {        
            intPart--;
        } 
        return (long) (number - intPart * divisor);
    }
    
    public static float mod(float number, float divisor) {        
        long quaotient = quaotient(number, divisor);
        
        long intPart = quaotient;
        if (quaotient < 0) {
            intPart--;
        } 
        return number - intPart * divisor;
    }
    
    public static double mod(double number, double divisor) {        
        long quaotient = quaotient(number, divisor);
        
        long intPart = quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return number - intPart * divisor;
    }
    
    public static BigInteger mod(BigInteger number, BigInteger divisor) {   
        if (number == null || divisor == null) {
            return BigInteger.valueOf(0);
        }
        long quaotient = quaotient(number, divisor);
        
        long intPart = quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return number.subtract(BigInteger.valueOf(intPart).multiply(divisor));
    }
    
    public static BigDecimal mod(BigDecimal number, BigDecimal divisor) {   
        if (number == null || divisor == null) {
            return BigDecimal.valueOf(0);
        }
        long quaotient = quaotient(number, divisor);
        
        long intPart = quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return number.subtract(BigDecimal.valueOf(intPart).multiply(divisor));
    }
    
    // SMALL
    public static byte small(byte[] values, int position) {
        byte result = 0;
        int index = position - 1; // arrays are 0-based
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
    public static short small(short[] values, int position) {
        short result = 0;
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
    public static int small(int[] values, int position) {
        int result = 0;
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
    public static long small(long[] values, int position) {
        long result = 0;
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
    public static float small(float[] values, int position) {
        float result = 0;
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
    public static double small(double[] values, int position) {
        double result = 0;
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
    public static BigInteger small(BigInteger[] values, int position) {
        BigInteger result = BigInteger.valueOf(0);
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
    public static BigDecimal small(BigDecimal[] values, int position) {
        BigDecimal result = BigDecimal.valueOf(0);
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    
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
    
    public static void aa() {
        
    }
}
