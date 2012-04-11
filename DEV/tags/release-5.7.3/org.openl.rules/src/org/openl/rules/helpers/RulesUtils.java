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

}
