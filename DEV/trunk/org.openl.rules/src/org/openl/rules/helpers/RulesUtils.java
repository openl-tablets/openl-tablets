/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.helpers;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.testmethod.OpenLUserRuntimeException;
import org.openl.util.ArrayTool;
import org.openl.util.DateTool;

/**
 * @author snshor
 */
public class RulesUtils {

    public static final String DEFAULT_DOUBLE_FORMAT = "#,##0.00";

    public static boolean contains(Object[] array, Object obj) {
        return ArrayUtils.contains(array, obj);
    }

    public static boolean contains(int[] array, int elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(long[] array, long elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(byte[] array, byte elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(short[] array, short elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(char[] array, char elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(float[] array, float elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(double[] array, double elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(boolean[] array, boolean elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Object[] ary1, Object[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(int[] ary1, int[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(byte[] ary1, byte[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(short[] ary1, short[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(long[] ary1, long[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(char[] ary1, char[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(float[] ary1, float[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(String[] ary1, String[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(double[] ary1, double[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(boolean[] ary1, boolean[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

//
    public static int indexOf(Object[] array, Object obj) {
        return ArrayUtils.indexOf(array, obj);
    }

    public static int indexOf(int[] array, int elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(long[] array, long elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(byte[] array, byte elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(short[] array, short elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(char[] array, char elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(float[] array, float elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(double[] array, double elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(boolean[] array, boolean elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static void error(String msg) {
        throw new OpenLUserRuntimeException(msg);
    }

    public static void error(Throwable t) throws Throwable {
        throw new OpenLUserRuntimeException(t);
    }

    public static String format(Date date) {
        return format(date, null);
    }

    public static String format(Date date, String format) {
        DateFormat df = format == null ? DateFormat.getDateInstance(DateFormat.SHORT) : new SimpleDateFormat(format);
        return df.format(date);
    }

    public static String format(double d) {
        return format(d, DEFAULT_DOUBLE_FORMAT);
    }

    public static String format(double d, String fmt) {
        DecimalFormat df = new DecimalFormat(fmt);
        return df.format(d);
    }

    public static String[] intersection(String[] ary1, String[] ary2) {
        return ArrayTool.intersection(ary1, ary2);
    }

    public static void out(String output) {
        System.out.println(output);
    }

    public static double parseFormattedDouble(String s) throws ParseException {
        return parseFormattedDouble(s, DEFAULT_DOUBLE_FORMAT);
    }

    public static double parseFormattedDouble(String s, String fmt) throws ParseException {
        DecimalFormat df = new DecimalFormat(fmt);
        return df.parse(s).doubleValue();
    }
    
    public static int absMonth(Date d) {
        return DateTool.absMonth(d);
    }

    public static int absQuarter(Date d) {
        return DateTool.absQuarter(d);
    }

    public static int dayDiff(Date d1, Date d2) {
        return DateTool.dayDiff(d1, d2);
    }

    public static int dayOfMonth(Date d) {
        return DateTool.dayOfMonth(d);
    }

    public static Date firstDateOfQuarter(int absQuarter) {
        return DateTool.firstDateOfQuarter(absQuarter);
    }

    public static Date lastDateOfQuarter(int absQuarter) {
        return DateTool.lastDateOfQuarter(absQuarter);
    }

    public static int lastDayOfMonth(Date d) {
        return DateTool.lastDayOfMonth(d);
    }

    public static int month(Date d) {
        return DateTool.month(d);
    }

    public static int monthDiff(Date d1, Date d2) {
        return DateTool.monthDiff(d1, d2);
    }
    
    public static int yearDiff(Date d1, Date d2) {
        return DateTool.yearDiff(d1, d2);
    }
    
    public static int weekDiff(Date d1, Date d2) {
        return DateTool.weekDiff(d1, d2);
    }

    public static int quarter(Date d) {
        return DateTool.quarter(d);
    }

    public static int year(Date d) {
        return DateTool.year(d);
    }
    
    // Math functions
    
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
    
    @SuppressWarnings("unchecked")
    public static Object max(Object[] values) {
        if (values == null) {
            throw new OpenlNotCheckedException(new IllegalArgumentException("The Array must not be null"));
        }            
        if (values.length == 0) {
            throw  new OpenlNotCheckedException(new IllegalArgumentException("Array cannot be empty."));
        }
        if (!(ClassUtils.isAssignable(values.getClass().getComponentType(), Number.class, true) && 
                ClassUtils.isAssignable(values.getClass().getComponentType(), Comparable.class, true))) {
            throw new OpenlNotCheckedException(new IllegalArgumentException("Income array must be comparable numeric."));
        }
        Comparable<Number>[] numberArray = (Comparable<Number>[])values;
        Number max = (Number) numberArray[0];
        for (int i = 0; i < numberArray.length; i++) {
            if (numberArray[i].compareTo(max) > 0) {
                max = (Number) numberArray[i];
            }
        }        
        return max;
    }
    
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
    
    @SuppressWarnings("unchecked")
    public static Object min(Object[] values) {
        if (values == null) {
            throw new OpenlNotCheckedException(new IllegalArgumentException("The Array must not be null"));
        }            
        if (values.length == 0) {
            throw  new OpenlNotCheckedException(new IllegalArgumentException("Array cannot be empty."));
        }
        if (!(ClassUtils.isAssignable(values.getClass().getComponentType(), Number.class, true) && 
                ClassUtils.isAssignable(values.getClass().getComponentType(), Comparable.class, true))) {
            throw new OpenlNotCheckedException(new IllegalArgumentException("Income array must be comparable numeric."));
        }
        Comparable<Number>[] numberArray = (Comparable<Number>[])values;
        Number min = (Number) numberArray[0];
        for (int i = 0; i < numberArray.length; i++) {
            if (numberArray[i].compareTo(min) < 0) {
                min = (Number) numberArray[i];
            }
        }        
        return min;
    }    

}
