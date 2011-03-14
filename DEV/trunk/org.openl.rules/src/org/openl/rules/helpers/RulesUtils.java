/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.openl.rules.testmethod.OpenLUserRuntimeException;
import org.openl.util.ArrayTool;
import org.openl.util.DateTool;
import org.openl.util.math.MathUtils;

/**
 * This class is connected to rules and all these methods can be used from rules.
 * 
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
    
    // MAX
    public static byte max(byte[] values) {
        return MathUtils.max(values);
    }
    
    public static char max(char[] values) {
        return MathUtils.max(values);
    }
    
    public static short max(short[] values) {
        return MathUtils.max(values);
    }
    
    public static int max(int[] values) {
        return MathUtils.max(values);
    }
    
    public static long max(long[] values) {
        return MathUtils.max(values);
    }
    
    public static float max(float[] values) {
        return MathUtils.max(values);
    }
    
    public static double max(double[] values) {
        return MathUtils.max(values);
    }
    
    public static BigInteger max(BigInteger[] values) {
        return MathUtils.max(values);
    }
    
    public static BigDecimal max(BigDecimal[] values) {
        return MathUtils.max(values);
    }
    
//    public static Object max(Object[] values) {
//        return MathUtils.max(values);
//    }
    
    //MIN    
    public static byte min(byte[] values) {
        return MathUtils.min(values);
    }
    
    public static char min(char[] values) {
        return MathUtils.min(values);
    }
    
    public static short min(short[] values) {
        return MathUtils.min(values);
    }
    
    public static int min(int[] values) {
        return MathUtils.min(values);
    }
    
    public static long min(long[] values) {
        return MathUtils.min(values);
    }
    
    public static float min(float[] values) {
        return MathUtils.min(values);
    }
    
    public static double min(double[] values) {
        return MathUtils.min(values);
    }
    
    public static BigInteger min(BigInteger[] values) {
        return MathUtils.min(values);
    }
    
    public static BigDecimal min(BigDecimal[] values) {
        return MathUtils.min(values);
    }

//    public static Object min(Object[] values) {
//        return MathUtils.min(values);
//    }
    
    //AVERAGE
    public static byte avg(byte[] values) {
        return MathUtils.avg(values);
    }
    
    public static short avg(short[] values) {
        return MathUtils.avg(values);      
    }
    
    public static int avg(int[] values) {
        return MathUtils.avg(values);       
    }
    
    public static long avg(long[] values) {
        return MathUtils.avg(values);
    }
    
    public static float avg(float[] values) {
        return MathUtils.avg(values);
    }
    
    public static double avg(double[] values) {
        return MathUtils.avg(values);
    }
    
    public static BigInteger avg(BigInteger[] values) {
        return MathUtils.avg(values);
    }
    
    public static BigDecimal avg(BigDecimal[] values) {
        return MathUtils.avg(values);
    }
    
    // SUMMARY
    public static byte sum(byte[] values) {
        return MathUtils.sum(values);        
    }
    
    public static short sum(short[] values) {
        return MathUtils.sum(values);        
    }
    
    public static int sum(int[] values) {
        return MathUtils.sum(values);
    }
    
    public static long sum(long[] values) {
        return MathUtils.sum(values);
    }
    
    public static float sum(float[] values) {
        return MathUtils.sum(values);
    }
    
    public static double sum(double[] values) {
        return MathUtils.sum(values);
    }
    
    public static BigInteger sum(BigInteger[] values) {
        return MathUtils.sum(values);
    }
    
    public static BigDecimal sum(BigDecimal[] values) {
        return MathUtils.sum(values);
    }
    
    //MEDIAN
    public static byte median(byte[] values) {
        return MathUtils.median(values);
    }
    
    public static short median(short[] values) {
        return MathUtils.median(values);
    }
    
    public static int median(int[] values) {
        return MathUtils.median(values);
    }
    
    public static long median(long[] values) {
        return MathUtils.median(values);
    }
    
    public static float median(float[] values) {
        return MathUtils.median(values);
    }
    
    public static double median(double[] values) {        
        return MathUtils.median(values);
    }
    
    //PRODUCT
    public static double product(byte[] values) {
        return MathUtils.product(values);
    }
    
    public static double product(short[] values) {
        return MathUtils.product(values);
    }
    
    public static double product(int[] values) {
        return MathUtils.product(values);
    }
    
    public static double product(long[] values) {
        return MathUtils.product(values);
    }
    
    public static double product(float[] values) {
        return MathUtils.product(values);
    }
    
    public static double product(double[] values) {
        return MathUtils.product(values);
    }
    
    public static BigInteger product(BigInteger[] values) {
        return MathUtils.product(values);
    }
    
    public static BigDecimal product(BigDecimal[] values) {
        return MathUtils.product(values);
    }
    
    // QUAOTIENT
    public static byte quaotient(byte number, byte divisor) {
        return MathUtils.quaotient(number, divisor);
    }
    
    public static short quaotient(short number, short divisor) {
        return MathUtils.quaotient(number, divisor);
    }
    
    public static int quaotient(int number, int divisor) {
        return MathUtils.quaotient(number, divisor);
    }
    
    public static long quaotient(long number, long divisor) {
        return MathUtils.quaotient(number, divisor);
    }
    
    public static long quaotient(float number, float divisor) {
        return MathUtils.quaotient(number, divisor);
    }
    
    public static long quaotient(double number, double divisor) {
        return MathUtils.quaotient(number, divisor);
    }    
    
    public static long quaotient(BigInteger number, BigInteger divisor) {
        return MathUtils.quaotient(number, divisor);
    }
    
    public static long quaotient(BigDecimal number, BigDecimal divisor) {
        return MathUtils.quaotient(number, divisor);
    }
    
    //MOD as in Excel
    public static byte mod(byte number, byte divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    public static short mod(short number, short divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    public static int mod(int number, int divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    public static long mod(long number, long divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    public static float mod(float number, float divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    public static double mod(double number, double divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    public static BigInteger mod(BigInteger number, BigInteger divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    public static BigDecimal mod(BigDecimal number, BigDecimal divisor) {
        return MathUtils.mod(number, divisor);
    }
    
    // SMALL
    public static byte small(byte[] values, int position) {
        return MathUtils.small(values, position);
    }
    
    public static short small(short[] values, int position) {
        return MathUtils.small(values, position);
    }
    
    public static int small(int[] values, int position) {
        return MathUtils.small(values, position);
    }
    
    public static long small(long[] values, int position) {
        return MathUtils.small(values, position);
    }
    
    public static float small(float[] values, int position) {
        return MathUtils.small(values, position);
    }
    
    public static double small(double[] values, int position) {
        return MathUtils.small(values, position);
    }
    
    // logical AND
    public static boolean and(boolean[] values) {
        return org.openl.util.BooleanUtils.and(values);
    }
    
    // logical OR
    public static boolean or(boolean[] values) {
        return BooleanUtils.xor(values);
    }    
}
