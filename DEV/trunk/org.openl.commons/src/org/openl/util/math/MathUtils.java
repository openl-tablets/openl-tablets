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

/**
 * The biggest part of methods is being generated. See org.openl.rules.gen module.
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
            return (double) (sum(values)/values.length);
        }
        return 0;
    }
	// AVERAGE for wrapper types
	public static java.lang.Byte avg(java.lang.Byte[] values) {
        if (ArrayTool.noNulls(values)) {
            return (byte) (sum(values) / 
            	java.lang.Byte.valueOf((byte) values.length));
        }
        return java.lang.Byte.valueOf("0");
    }
	public static java.lang.Short avg(java.lang.Short[] values) {
        if (ArrayTool.noNulls(values)) {
            return (short) (sum(values) / 
            	java.lang.Short.valueOf((short) values.length));
        }
        return java.lang.Short.valueOf("0");
    }
	public static java.lang.Integer avg(java.lang.Integer[] values) {
        if (ArrayTool.noNulls(values)) {
            return (int) (sum(values) / 
            	java.lang.Integer.valueOf((int) values.length));
        }
        return java.lang.Integer.valueOf("0");
    }
	public static java.lang.Long avg(java.lang.Long[] values) {
        if (ArrayTool.noNulls(values)) {
            return (long) (sum(values) / 
            	java.lang.Long.valueOf((long) values.length));
        }
        return java.lang.Long.valueOf("0");
    }
	public static java.lang.Float avg(java.lang.Float[] values) {
        if (ArrayTool.noNulls(values)) {
            return (float) (sum(values) / 
            	java.lang.Float.valueOf((float) values.length));
        }
        return java.lang.Float.valueOf("0");
    }
	public static java.lang.Double avg(java.lang.Double[] values) {
        if (ArrayTool.noNulls(values)) {
            return (double) (sum(values) / 
            	java.lang.Double.valueOf((double) values.length));
        }
        return java.lang.Double.valueOf("0");
    }

	// SMALL for primitives	
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
        int index = position - 1; // arrays are 0-based
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    public static int small(int[] values, int position) {
        int result = 0;
        int index = position - 1; // arrays are 0-based
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    public static long small(long[] values, int position) {
        long result = 0;
        int index = position - 1; // arrays are 0-based
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    public static float small(float[] values, int position) {
        float result = 0;
        int index = position - 1; // arrays are 0-based
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
    public static double small(double[] values, int position) {
        double result = 0;
        int index = position - 1; // arrays are 0-based
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }

	 public static java.lang.Byte small(java.lang.Byte[] values, int position) {
        java.lang.Byte result = java.lang.Byte.valueOf("0");
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
	 public static java.lang.Short small(java.lang.Short[] values, int position) {
        java.lang.Short result = java.lang.Short.valueOf("0");
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
	 public static java.lang.Integer small(java.lang.Integer[] values, int position) {
        java.lang.Integer result = java.lang.Integer.valueOf("0");
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
	 public static java.lang.Long small(java.lang.Long[] values, int position) {
        java.lang.Long result = java.lang.Long.valueOf("0");
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
	 public static java.lang.Float small(java.lang.Float[] values, int position) {
        java.lang.Float result = java.lang.Float.valueOf("0");
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
        return result;
    }
	 public static java.lang.Double small(java.lang.Double[] values, int position) {
        java.lang.Double result = java.lang.Double.valueOf("0");
        int index = position - 1; 
        if (values != null && values.length > index && index >= 0) {
            Arrays.sort(values);
            result = values[index];
        }
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
        java.lang.Byte sum = java.lang.Byte.valueOf("0");
        if (ArrayTool.noNulls(values)) {
            for (java.lang.Byte value : values) {
                sum = (byte) (sum + value);
            }            
        }
        return sum;
    }
	public static java.lang.Short sum(java.lang.Short[] values) {
        java.lang.Short sum = java.lang.Short.valueOf("0");
        if (ArrayTool.noNulls(values)) {
            for (java.lang.Short value : values) {
                sum = (short) (sum + value);
            }            
        }
        return sum;
    }
	public static java.lang.Integer sum(java.lang.Integer[] values) {
        java.lang.Integer sum = java.lang.Integer.valueOf("0");
        if (ArrayTool.noNulls(values)) {
            for (java.lang.Integer value : values) {
                sum = (int) (sum + value);
            }            
        }
        return sum;
    }
	public static java.lang.Long sum(java.lang.Long[] values) {
        java.lang.Long sum = java.lang.Long.valueOf("0");
        if (ArrayTool.noNulls(values)) {
            for (java.lang.Long value : values) {
                sum = (long) (sum + value);
            }            
        }
        return sum;
    }
	public static java.lang.Float sum(java.lang.Float[] values) {
        java.lang.Float sum = java.lang.Float.valueOf("0");
        if (ArrayTool.noNulls(values)) {
            for (java.lang.Float value : values) {
                sum = (float) (sum + value);
            }            
        }
        return sum;
    }
	public static java.lang.Double sum(java.lang.Double[] values) {
        java.lang.Double sum = java.lang.Double.valueOf("0");
        if (ArrayTool.noNulls(values)) {
            for (java.lang.Double value : values) {
                sum = (double) (sum + value);
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

	// MEDIAN for wrapper types
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
        double res = 0;
        if (ArrayTool.noNulls(values)) {
            res = 1;
            for (java.lang.Byte value : values) {
                res = res * value;
            }
        }
        return res;
    }
	public static double product(java.lang.Short[] values) {
        double res = 0;
        if (ArrayTool.noNulls(values)) {
            res = 1;
            for (java.lang.Short value : values) {
                res = res * value;
            }
        }
        return res;
    }
	public static double product(java.lang.Integer[] values) {
        double res = 0;
        if (ArrayTool.noNulls(values)) {
            res = 1;
            for (java.lang.Integer value : values) {
                res = res * value;
            }
        }
        return res;
    }
	public static double product(java.lang.Long[] values) {
        double res = 0;
        if (ArrayTool.noNulls(values)) {
            res = 1;
            for (java.lang.Long value : values) {
                res = res * value;
            }
        }
        return res;
    }
	public static double product(java.lang.Float[] values) {
        double res = 0;
        if (ArrayTool.noNulls(values)) {
            res = 1;
            for (java.lang.Float value : values) {
                res = res * value;
            }
        }
        return res;
    }
	public static double product(java.lang.Double[] values) {
        double res = 0;
        if (ArrayTool.noNulls(values)) {
            res = 1;
            for (java.lang.Double value : values) {
                res = res * value;
            }
        }
        return res;
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
    

	// SLICE for wrapper types
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
	
	// MOD is implemented as in Excel.
	public static byte mod(byte number, byte divisor) {        
        long quaotient = quaotient(number, divisor);
        
        byte intPart = (byte) quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return (byte) (number - intPart * divisor);
    }
	public static short mod(short number, short divisor) {        
        long quaotient = quaotient(number, divisor);
        
        short intPart = (short) quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return (short) (number - intPart * divisor);
    }
	public static int mod(int number, int divisor) {        
        long quaotient = quaotient(number, divisor);
        
        int intPart = (int) quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return (int) (number - intPart * divisor);
    }
	public static long mod(long number, long divisor) {        
        long quaotient = quaotient(number, divisor);
        
        long intPart = (long) quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return (long) (number - intPart * divisor);
    }
	public static float mod(float number, float divisor) {        
        long quaotient = quaotient(number, divisor);
        
        float intPart = (float) quaotient;
        if (quaotient < 0) {            
            intPart--;
        } 
        return (float) (number - intPart * divisor);
    }
	public static double mod(double number, double divisor) {        
        long quaotient = quaotient(number, divisor);
        
        double intPart = (double) quaotient;
        if (quaotient < 0) {            
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
	
	// QUAOTIENT
	public static long quaotient(byte number, byte divisor) {
        return (long) (number / divisor);
    }
	public static long quaotient(short number, short divisor) {
        return (long) (number / divisor);
    }
	public static long quaotient(int number, int divisor) {
        return (long) (number / divisor);
    }
	public static long quaotient(long number, long divisor) {
        return (long) (number / divisor);
    }
	public static long quaotient(float number, float divisor) {
        return (long) (number / divisor);
    }
	public static long quaotient(double number, double divisor) {
        return (long) (number / divisor);
    }

	// QUAOTIENT for wrapper types
	public static long quaotient(java.lang.Byte number, java.lang.Byte divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return quaotient((byte) number, (byte) divisor);
    }
 	public static long quaotient(java.lang.Short number, java.lang.Short divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return quaotient((short) number, (short) divisor);
    }
 	public static long quaotient(java.lang.Integer number, java.lang.Integer divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return quaotient((int) number, (int) divisor);
    }
 	public static long quaotient(java.lang.Long number, java.lang.Long divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return quaotient((long) number, (long) divisor);
    }
 	public static long quaotient(java.lang.Float number, java.lang.Float divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return quaotient((float) number, (float) divisor);
    }
 	public static long quaotient(java.lang.Double number, java.lang.Double divisor) {
        if (number == null || divisor == null) {
            return 0;
        }        
        return quaotient((double) number, (double) divisor);
    }
  
 	// SORT
 	public static byte[] sort(byte[] values) {
		Arrays.sort(values);
        return values;
    }
	public static short[] sort(short[] values) {
		Arrays.sort(values);
        return values;
    }
	public static int[] sort(int[] values) {
		Arrays.sort(values);
        return values;
    }
	public static long[] sort(long[] values) {
		Arrays.sort(values);
        return values;
    }
	public static float[] sort(float[] values) {
		Arrays.sort(values);
        return values;
    }
	public static double[] sort(double[] values) {
		Arrays.sort(values);
        return values;
    }

	// SORT for wrapper types
	public static java.lang.Byte[] sort(java.lang.Byte[] values) {
		Arrays.sort(values);
        return values;
    }
 	public static java.lang.Short[] sort(java.lang.Short[] values) {
		Arrays.sort(values);
        return values;
    }
 	public static java.lang.Integer[] sort(java.lang.Integer[] values) {
		Arrays.sort(values);
        return values;
    }
 	public static java.lang.Long[] sort(java.lang.Long[] values) {
		Arrays.sort(values);
        return values;
    }
 	public static java.lang.Float[] sort(java.lang.Float[] values) {
		Arrays.sort(values);
        return values;
    }
 	public static java.lang.Double[] sort(java.lang.Double[] values) {
		Arrays.sort(values);
        return values;
    }
       	// <<< END INSERT Functions >>> 

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
	
	public static BigInteger divide(BigInteger number, BigInteger divisor) {
        if (number == null || divisor == null) {
            return null;
        }
        return number.divide(divisor);
    }
	
//	public static BigInteger divide(Number number, Number divisor) {
//        if (number == null || divisor == null) {
//            return null;
//        }
//        return number / divisor;
//    }
	
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
    
    // MIN for big types    
    public static boolean min(BigInteger value1, BigInteger value2) {
        return value1.compareTo(value2) < 0;
    }
    
    public static boolean min(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) < 0;
    }
    
    //MIN IN ARRAY 
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
    
    // AVERAGE
    public static BigInteger avg(BigInteger[] values) {
        if (ArrayTool.noNulls(values)) {
            return divide(sum(values), BigInteger.valueOf(values.length));
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
    public static double median(double[] values) {        
        Median median = new Median();
        return median.evaluate(values, 0, values.length);
    }
    
    // MEDIAN for big types
    public static BigInteger median(BigInteger[] values) {    
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet", 
            values.getClass().getName()));
    }
    
    public static BigDecimal median(BigDecimal[] values) {
        // TODO implement
        throw new NotImplementedException(String.format("Method median for %s is not implemented yet", 
            values.getClass().getName()));
    }
    
    // PRODUCT
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
    
    // QUAOTIENT for big types
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
    
    // MOD for big types       
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
    
    // SMALL for big types
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
    
    // SLICE for big types
    public static BigInteger[] slice(BigInteger[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }
    
    public static BigInteger[] slice(BigInteger[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (BigInteger[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }
    
    public static BigDecimal[] slice(BigDecimal[] values, int startIndexInclusive) {
        if (values != null) {
            return slice(values, startIndexInclusive, values.length);
        }
        return null;
    }
    
    public static BigDecimal[] slice(BigDecimal[] values, int startIndexInclusive, int endIndexExclusive) {
        if (values != null) {
            return (BigDecimal[]) ArrayUtils.subarray(values, startIndexInclusive, endIndexExclusive);
        }
        return null;
    }
    
    // SORT for big ypes
    public static BigInteger[] sort(BigInteger[] values) {
        Arrays.sort(values);
        return values;
    }
    
    public static BigDecimal[] sort(BigDecimal[] values) {
        Arrays.sort(values);
        return values;
    }
}
