/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ObjectValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.rules.testmethod.OpenLUserRuntimeException;
import org.openl.util.ArrayTool;
import org.openl.util.DateTool;
import org.openl.util.math.MathUtils;

/**
 * This class is connected to rules and all these methods can be used from
 * rules. The biggest part of methods is being generated. See
 * org.openl.rules.gen module.
 * 
 * @author snshor
 */
public class RulesUtils {

    public static final String DEFAULT_DOUBLE_FORMAT = "#,##0.00";

    public static final double E = Math.E;
    public static final double PI = Math.PI;

    // <<< INSERT Functions >>>
    // MAX
    public static java.lang.Byte max(java.lang.Byte[] values) {
        return (java.lang.Byte) MathUtils.max(values);
    }
    /** Returns max short value
     * 
     * @param values Short array 
     * @return max short
     */
    public static java.lang.Short max(java.lang.Short[] values) {
        return (java.lang.Short) MathUtils.max(values);
    }

    public static java.lang.Integer max(java.lang.Integer[] values) {
        return (java.lang.Integer) MathUtils.max(values);
    }

    public static java.lang.Long max(java.lang.Long[] values) {
        return (java.lang.Long) MathUtils.max(values);
    }

    public static java.lang.Float max(java.lang.Float[] values) {
        return (java.lang.Float) MathUtils.max(values);
    }

    public static java.lang.Double max(java.lang.Double[] values) {
        return (java.lang.Double) MathUtils.max(values);
    }

    public static java.math.BigInteger max(java.math.BigInteger[] values) {
        return (java.math.BigInteger) MathUtils.max(values);
    }

    public static java.math.BigDecimal max(java.math.BigDecimal[] values) {
        return (java.math.BigDecimal) MathUtils.max(values);
    }

    public static byte max(byte[] values) {
        return (byte) MathUtils.max(values);
    }

    public static short max(short[] values) {
        return (short) MathUtils.max(values);
    }

    public static int max(int[] values) {
        return (int) MathUtils.max(values);
    }

    public static long max(long[] values) {
        return (long) MathUtils.max(values);
    }

    public static float max(float[] values) {
        return (float) MathUtils.max(values);
    }

    public static double max(double[] values) {
        return (double) MathUtils.max(values);
    }

    // MIN
    public static java.lang.Byte min(java.lang.Byte[] values) {
        return (java.lang.Byte) MathUtils.min(values);
    }

    public static java.lang.Short min(java.lang.Short[] values) {
        return (java.lang.Short) MathUtils.min(values);
    }

    public static java.lang.Integer min(java.lang.Integer[] values) {
        return (java.lang.Integer) MathUtils.min(values);
    }

    public static java.lang.Long min(java.lang.Long[] values) {
        return (java.lang.Long) MathUtils.min(values);
    }

    public static java.lang.Float min(java.lang.Float[] values) {
        return (java.lang.Float) MathUtils.min(values);
    }

    public static java.lang.Double min(java.lang.Double[] values) {
        return (java.lang.Double) MathUtils.min(values);
    }

    public static java.math.BigInteger min(java.math.BigInteger[] values) {
        return (java.math.BigInteger) MathUtils.min(values);
    }

    public static java.math.BigDecimal min(java.math.BigDecimal[] values) {
        return (java.math.BigDecimal) MathUtils.min(values);
    }

    public static byte min(byte[] values) {
        return (byte) MathUtils.min(values);
    }

    public static short min(short[] values) {
        return (short) MathUtils.min(values);
    }

    public static int min(int[] values) {
        return (int) MathUtils.min(values);
    }

    public static long min(long[] values) {
        return (long) MathUtils.min(values);
    }

    public static float min(float[] values) {
        return (float) MathUtils.min(values);
    }

    public static double min(double[] values) {
        return (double) MathUtils.min(values);
    }

    // SUM
    public static java.lang.Byte sum(java.lang.Byte[] values) {
        return (java.lang.Byte) MathUtils.sum(values);
    }

    public static java.lang.Short sum(java.lang.Short[] values) {
        return (java.lang.Short) MathUtils.sum(values);
    }

    public static java.lang.Integer sum(java.lang.Integer[] values) {
        return (java.lang.Integer) MathUtils.sum(values);
    }

    public static java.lang.Long sum(java.lang.Long[] values) {
        return (java.lang.Long) MathUtils.sum(values);
    }

    public static java.lang.Float sum(java.lang.Float[] values) {
        return (java.lang.Float) MathUtils.sum(values);
    }

    public static java.lang.Double sum(java.lang.Double[] values) {
        return (java.lang.Double) MathUtils.sum(values);
    }

    public static java.math.BigInteger sum(java.math.BigInteger[] values) {
        return (java.math.BigInteger) MathUtils.sum(values);
    }

    public static java.math.BigDecimal sum(java.math.BigDecimal[] values) {
        return (java.math.BigDecimal) MathUtils.sum(values);
    }

    public static byte sum(byte[] values) {
        return (byte) MathUtils.sum(values);
    }

    public static short sum(short[] values) {
        return (short) MathUtils.sum(values);
    }

    public static int sum(int[] values) {
        return (int) MathUtils.sum(values);
    }

    public static long sum(long[] values) {
        return (long) MathUtils.sum(values);
    }

    public static float sum(float[] values) {
        return (float) MathUtils.sum(values);
    }

    public static double sum(double[] values) {
        return (double) MathUtils.sum(values);
    }

    // AVERAGE
    public static java.lang.Byte avg(java.lang.Byte[] values) {
        return MathUtils.avg(values);
    }

    public static java.lang.Short avg(java.lang.Short[] values) {
        return MathUtils.avg(values);
    }

    public static java.lang.Integer avg(java.lang.Integer[] values) {
        return MathUtils.avg(values);
    }

    public static java.lang.Long avg(java.lang.Long[] values) {
        return MathUtils.avg(values);
    }

    public static java.lang.Float avg(java.lang.Float[] values) {
        return MathUtils.avg(values);
    }

    public static java.lang.Double avg(java.lang.Double[] values) {
        return MathUtils.avg(values);
    }

    public static java.math.BigInteger avg(java.math.BigInteger[] values) {
        return MathUtils.avg(values);
    }

    public static java.math.BigDecimal avg(java.math.BigDecimal[] values) {
        return MathUtils.avg(values);
    }

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

    // SMALL
    public static java.lang.Byte small(java.lang.Byte[] values, int position) {
        return MathUtils.small(values, position);
    }

    public static java.lang.Short small(java.lang.Short[] values, int position) {
        return MathUtils.small(values, position);
    }

    public static java.lang.Integer small(java.lang.Integer[] values, int position) {
        return MathUtils.small(values, position);
    }

    public static java.lang.Long small(java.lang.Long[] values, int position) {
        return MathUtils.small(values, position);
    }

    public static java.lang.Float small(java.lang.Float[] values, int position) {
        return MathUtils.small(values, position);
    }

    public static java.lang.Double small(java.lang.Double[] values, int position) {
        return MathUtils.small(values, position);
    }

    public static java.math.BigInteger small(java.math.BigInteger[] values, int position) {
        return MathUtils.small(values, position);
    }

    public static java.math.BigDecimal small(java.math.BigDecimal[] values, int position) {
        return MathUtils.small(values, position);
    }

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

    // BIG
    public static java.lang.Byte big(java.lang.Byte[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static java.lang.Short big(java.lang.Short[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static java.lang.Integer big(java.lang.Integer[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static java.lang.Long big(java.lang.Long[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static java.lang.Float big(java.lang.Float[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static java.lang.Double big(java.lang.Double[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static java.math.BigInteger big(java.math.BigInteger[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static java.math.BigDecimal big(java.math.BigDecimal[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static byte big(byte[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static short big(short[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static int big(int[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static long big(long[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static float big(float[] values, int position) {
        return MathUtils.big(values, position);
    }

    public static double big(double[] values, int position) {
        return MathUtils.big(values, position);
    }

    // MEDIAN
    public static java.lang.Byte median(java.lang.Byte[] values) {
        return MathUtils.median(values);
    }

    public static java.lang.Short median(java.lang.Short[] values) {
        return MathUtils.median(values);
    }

    public static java.lang.Integer median(java.lang.Integer[] values) {
        return MathUtils.median(values);
    }

    public static java.lang.Long median(java.lang.Long[] values) {
        return MathUtils.median(values);
    }

    public static java.lang.Float median(java.lang.Float[] values) {
        return MathUtils.median(values);
    }

    public static java.lang.Double median(java.lang.Double[] values) {
        return MathUtils.median(values);
    }

    public static java.math.BigInteger median(java.math.BigInteger[] values) {
        return MathUtils.median(values);
    }

    public static java.math.BigDecimal median(java.math.BigDecimal[] values) {
        return MathUtils.median(values);
    }

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

    // QUAOTIENT
    public static long quotient(java.lang.Byte number, java.lang.Byte divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(java.lang.Short number, java.lang.Short divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(java.lang.Integer number, java.lang.Integer divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(java.lang.Long number, java.lang.Long divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(java.lang.Float number, java.lang.Float divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(java.lang.Double number, java.lang.Double divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(java.math.BigInteger number, java.math.BigInteger divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(java.math.BigDecimal number, java.math.BigDecimal divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(byte number, byte divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(short number, short divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(int number, int divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(long number, long divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(float number, float divisor) {
        return MathUtils.quotient(number, divisor);
    }

    public static long quotient(double number, double divisor) {
        return MathUtils.quotient(number, divisor);
    }

    // MOD as in Excel
    public static java.lang.Byte mod(java.lang.Byte number, java.lang.Byte divisor) {
        return MathUtils.mod(number, divisor);
    }

    public static java.lang.Short mod(java.lang.Short number, java.lang.Short divisor) {
        return MathUtils.mod(number, divisor);
    }

    public static java.lang.Integer mod(java.lang.Integer number, java.lang.Integer divisor) {
        return MathUtils.mod(number, divisor);
    }

    public static java.lang.Long mod(java.lang.Long number, java.lang.Long divisor) {
        return MathUtils.mod(number, divisor);
    }

    public static java.lang.Float mod(java.lang.Float number, java.lang.Float divisor) {
        return MathUtils.mod(number, divisor);
    }

    public static java.lang.Double mod(java.lang.Double number, java.lang.Double divisor) {
        return MathUtils.mod(number, divisor);
    }

    public static java.math.BigInteger mod(java.math.BigInteger number, java.math.BigInteger divisor) {
        return MathUtils.mod(number, divisor);
    }

    public static java.math.BigDecimal mod(java.math.BigDecimal number, java.math.BigDecimal divisor) {
        return MathUtils.mod(number, divisor);
    }

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

    // SLICE
    public static java.lang.Byte[] slice(java.lang.Byte[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.lang.Byte[] slice(java.lang.Byte[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static java.lang.Short[] slice(java.lang.Short[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.lang.Short[] slice(java.lang.Short[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static java.lang.Integer[] slice(java.lang.Integer[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.lang.Integer[] slice(java.lang.Integer[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static java.lang.Long[] slice(java.lang.Long[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.lang.Long[] slice(java.lang.Long[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static java.lang.Float[] slice(java.lang.Float[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.lang.Float[] slice(java.lang.Float[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static java.lang.Double[] slice(java.lang.Double[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.lang.Double[] slice(java.lang.Double[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static java.math.BigInteger[] slice(java.math.BigInteger[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.math.BigInteger[] slice(java.math.BigInteger[] values,
            int startIndexInclusive,
            int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static java.math.BigDecimal[] slice(java.math.BigDecimal[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static java.math.BigDecimal[] slice(java.math.BigDecimal[] values,
            int startIndexInclusive,
            int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static byte[] slice(byte[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static byte[] slice(byte[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static short[] slice(short[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static short[] slice(short[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static int[] slice(int[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static int[] slice(int[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static long[] slice(long[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static long[] slice(long[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static float[] slice(float[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static float[] slice(float[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    public static double[] slice(double[] values, int startIndexInclusive) {
        return MathUtils.slice(values, startIndexInclusive);
    }

    public static double[] slice(double[] values, int startIndexInclusive, int endIndexExclusive) {
        return MathUtils.slice(values, startIndexInclusive, endIndexExclusive);
    }

    // SORT
    public static java.lang.Byte[] sort(java.lang.Byte[] values) {
        return MathUtils.sort(values);
    }

    public static java.lang.Short[] sort(java.lang.Short[] values) {
        return MathUtils.sort(values);
    }

    public static java.lang.Integer[] sort(java.lang.Integer[] values) {
        return MathUtils.sort(values);
    }

    public static java.lang.Long[] sort(java.lang.Long[] values) {
        return MathUtils.sort(values);
    }

    public static java.lang.Float[] sort(java.lang.Float[] values) {
        return MathUtils.sort(values);
    }

    public static java.lang.Double[] sort(java.lang.Double[] values) {
        return MathUtils.sort(values);
    }

    public static java.math.BigInteger[] sort(java.math.BigInteger[] values) {
        return MathUtils.sort(values);
    }

    public static java.math.BigDecimal[] sort(java.math.BigDecimal[] values) {
        return MathUtils.sort(values);
    }

    public static byte[] sort(byte[] values) {
        return MathUtils.sort(values);
    }

    public static short[] sort(short[] values) {
        return MathUtils.sort(values);
    }

    public static int[] sort(int[] values) {
        return MathUtils.sort(values);
    }

    public static long[] sort(long[] values) {
        return MathUtils.sort(values);
    }

    public static float[] sort(float[] values) {
        return MathUtils.sort(values);
    }

    public static double[] sort(double[] values) {
        return MathUtils.sort(values);
    }

    public static ByteValue[] sort(ByteValue[] values) {
        return ByteValue.sort(values);
    }

    public static ShortValue[] sort(ShortValue[] values) {
        return ShortValue.sort(values);
    }

    public static IntValue[] sort(IntValue[] values) {
        return IntValue.sort(values);
    }

    public static LongValue[] sort(LongValue[] values) {
        return LongValue.sort(values);
    }

    public static FloatValue[] sort(FloatValue[] values) {
        return FloatValue.sort(values);
    }

    public static DoubleValue[] sort(DoubleValue[] values) {
        return DoubleValue.sort(values);
    }

    public static BigDecimalValue[] sort(BigDecimalValue[] values) {
        return BigDecimalValue.sort(values);
    }

    public static BigIntegerValue[] sort(BigIntegerValue[] values) {
        return BigIntegerValue.sort(values);
    }

    public static StringValue[] sort(StringValue[] values) {
        return StringValue.sort(values);
    }

    public static ObjectValue[] sort(ObjectValue[] values) {
        return ObjectValue.sort(values);
    }

    public static String[] sort(String[] values) {
        return ArrayTool.sort(values);
    }

    public static Date[] sort(Date[] values) {
        return ArrayTool.sort(values);
    }

    // <<< END INSERT Functions >>>

    // <<< Contains Functions >>>

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

    public static boolean contains(String str, String searchStr) {
        return StringUtils.contains(str, searchStr);
    }

    public static boolean contains(String str, char searchChar) {
        return StringUtils.contains(str, searchChar);
    }

    public static boolean containsAny(String str, char[] chars) {
        return StringUtils.containsAny(str, chars);
    }

    public static boolean containsAny(String str, String searchChars) {
        return StringUtils.containsAny(str, searchChars);
    }

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

    public static boolean noNulls(Object[] values) {
        return ArrayTool.noNulls(values);
    }

    public static void error(String msg) {
        throw new OpenLUserRuntimeException(msg);
    }

    public static void error(Throwable t) throws Throwable {
        throw new OpenLUserRuntimeException(t);
    }
    
    
    /**
     * method dateToString(Date date) should be used
     * @param date
     * @return formated date value
     */
    @Deprecated
    public static String format(Date date) {
        return dateToString(date);
    }
    
    /**
     * method dateToString (Date date, String format) should be used
     * @param date
     * @param format
     * @return String formated date value
     */
    @Deprecated
    public static String format(Date date, String format) {
        return dateToString(date, format);
    }

    /**
     * converts a date to the String according dateFormat
     * 
     * @param date
     * @param dateFormat
     * @return String formated date value
     */
    public static String dateToString(Date date, String dateFormat) {
        return DateTool.dateToString(date, dateFormat);
    }

    /**
     * converts a date to the String according dateFormat
     * 
     * @see DateTool.dateToString();
     * @param date
     * @return String formated date value
     */
    public static String dateToString(Date date) {
        return DateTool.dateToString(date);
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

    public static void out(Object output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(byte output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(short output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(int output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(long output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(float output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(double output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(char output) {
        System.out.println(String.valueOf(output));
    }

    public static void out(boolean output) {
        System.out.println(String.valueOf(output));
    }

    /**
     * Parse the represented string value to the double. Uses default Locale for
     * it.
     * 
     * Shouldn`t be used.
     */
    @Deprecated
    public static double parseFormattedDouble(String s) throws ParseException {
        return parseFormattedDouble(s, DEFAULT_DOUBLE_FORMAT);
    }

    /**
     * Parse the represented string value to the double. Uses default Locale for
     * it. See {@link DecimalFormat#DecimalFormat(String)}
     * 
     * Shouldn`t be used.
     */
    @Deprecated
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

    public static int dayOfWeek(Date d) {
        return DateTool.dayOfWeek(d);
    }

    public static int dayOfYear(Date d) {
        return DateTool.dayOfYear(d);
    }

    public static int weekOfYear(Date d) {
        return DateTool.weekOfYear(d);
    }

    public static int weekOfMonth(Date d) {
        return DateTool.weekOfMonth(d);
    }

    public static int second(Date d) {
        return DateTool.second(d);
    }

    public static int minute(Date d) {
        return DateTool.minute(d);
    }

    /**
     * @param d Date
     * @return hour from 0 to 12
     */
    public static int hour(Date d) {
        return DateTool.hour(d);
    }

    /**
     * @param d Date
     * @return hour from 0 to 24
     */
    public static int hourOfDay(Date d) {
        return DateTool.hourOfDay(d);
    }

    public static String amPm(Date d) {
        return DateTool.amPm(d);
    }

    // Math functions

    // PRODUCT
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

    public static double product(Byte[] values) {
        return MathUtils.product(values);
    }

    public static double product(Short[] values) {
        return MathUtils.product(values);
    }

    public static double product(Integer[] values) {
        return MathUtils.product(values);
    }

    public static double product(Long[] values) {
        return MathUtils.product(values);
    }

    public static double product(Float[] values) {
        return MathUtils.product(values);
    }

    public static double product(Double[] values) {
        return MathUtils.product(values);
    }

    public static BigInteger product(BigInteger[] values) {
        return MathUtils.product(values);
    }

    public static BigDecimal product(BigDecimal[] values) {
        return MathUtils.product(values);
    }

    // logical AND
    public static boolean allTrue(boolean[] values) {
        return org.openl.util.BooleanUtils.and(values);
    }

    public static boolean allTrue(Boolean[] values) {
        return org.openl.util.BooleanUtils.and(values);
    }

    // Exclusive or
    public static boolean xor(boolean[] values) {
        return org.openl.util.BooleanUtils.xor(values);
    }

    public static boolean xor(Boolean[] values) {
        return org.openl.util.BooleanUtils.xor(values);
    }

    // or
    public static boolean anyTrue(boolean[] values) {
        return org.openl.util.BooleanUtils.or(values);
    }

    public static boolean anyTrue(Boolean[] values) {
        return org.openl.util.BooleanUtils.or(values);
    }

    public static long round(double value) {
        return Math.round(value);
    }

    public static int round(float value) {
        return Math.round(value);
    }

    public static double round(double value, int scale) {
        return org.apache.commons.math.util.MathUtils.round(value, scale);
    }

    public static float round(float value, int scale) {
        return org.apache.commons.math.util.MathUtils.round(value, scale);
    }

    public static double round(double value, int scale, int roundingMethod) {
        return org.apache.commons.math.util.MathUtils.round(value, scale, roundingMethod);
    }

    public static float round(float value, int scale, int roundingMethod) {
        return org.apache.commons.math.util.MathUtils.round(value, scale, roundingMethod);
    }

    // added for BA`s, who don`t know about the possibilities of
    // BigDecimal
    public static BigDecimal round(BigDecimal value) {
        return round(value, 0);
    }

    // added for BA`s, who don`t know about the possibilities of
    // BigDecimal
    public static BigDecimal round(BigDecimal value, int scale) {
        return round(value, scale, BigDecimal.ROUND_HALF_UP);
    }

    // added for BA`s, who don`t know about the possibilities of
    // BigDecimal
    public static BigDecimal round(BigDecimal value, int scale, int roundingMethod) {
        return value.setScale(scale, roundingMethod);
    }

    public static <T> T[] removeNulls(T[] array) {
        return ArrayTool.removeNulls(array);
    }

    // Delegation Methods from java.lang.Math class
    public static double abs(double a) {
        return Math.abs(a);
    }

    public static float abs(float a) {
        return Math.abs(a);
    }

    public static int abs(int a) {
        return Math.abs(a);
    }

    public static long abs(long a) {
        return Math.abs(a);
    }

    public static double acos(double a) {
        return Math.acos(a);
    }

    public static double asin(double a) {
        return Math.asin(a);
    }

    public static double atan(double a) {
        return Math.atan(a);
    }

    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public static double cbrt(double a) {
        return Math.cbrt(a);
    }

    public static double ceil(double a) {
        return Math.ceil(a);
    }

    public static double copySign(double magnitude, double sign) {
        return Math.copySign(magnitude, sign);
    }

    public static float copySign(float magnitude, float sign) {
        return Math.copySign(magnitude, sign);
    }

    public static double cos(double a) {
        return Math.cos(a);
    }

    public static double cosh(double x) {
        return Math.cosh(x);
    }

    public static double exp(double a) {
        return Math.exp(a);
    }

    public static double expm1(double x) {
        return Math.expm1(x);
    }

    public static double floor(double a) {
        return Math.floor(a);
    }

    public static int getExponent(double d) {
        return Math.getExponent(d);
    }

    public static int getExponent(float f) {
        return Math.getExponent(f);
    }

    public static double getExponent(double x, double y) {
        return Math.hypot(x, y);
    }

    public static double IEEEremainder(double f1, double f2) {
        return Math.IEEEremainder(f1, f2);
    }

    public static double log(double a) {
        return Math.log(a);
    }

    public static double log10(double a) {
        return Math.log10(a);
    }

    public static double log1p(double x) {
        return Math.log1p(x);
    }

    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    public static Integer max(Integer a, Integer b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    public static Double max(Double a, Double b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    public static Float max(Float a, Float b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    public static Long max(Long a, Long b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    public static java.math.BigInteger max(java.math.BigInteger a, java.math.BigInteger b) {
        return a == null ? b : (b == null ? a : a.max(b));
    }

    public static java.math.BigDecimal max(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b : (b == null ? a : a.max(b));
    }

    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    public static Integer min(Integer a, Integer b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    public static Double min(Double a, Double b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    public static Float min(Float a, Float b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    public static Long min(Long a, Long b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    public static java.math.BigInteger min(java.math.BigInteger a, java.math.BigInteger b) {
        return a == null ? b : (b == null ? a : a.min(b));
    }

    public static java.math.BigDecimal min(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b : (b == null ? a : a.min(b));
    }

    public static double nextAfter(double start, double direction) {
        return Math.nextAfter(start, direction);
    }

    public static float nextAfter(float start, float direction) {
        return Math.nextAfter(start, direction);
    }

    public static float nextAfter(float f) {
        return Math.nextUp(f);
    }

    public static double nextAfter(double d) {
        return Math.nextUp(d);
    }

    public static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    public static double random() {
        return Math.random();
    }

    public static double rint(double a) {
        return Math.rint(a);
    }

    public static double scalb(double d, int scaleFactor) {
        return Math.scalb(d, scaleFactor);
    }

    public static float scalb(float f, int scaleFactor) {
        return Math.scalb(f, scaleFactor);
    }

    public static double signum(double d) {
        return Math.signum(d);
    }

    public static double signum(float f) {
        return Math.signum(f);
    }

    public static double sin(double a) {
        return Math.sin(a);
    }

    public static double sinh(double x) {
        return Math.sinh(x);
    }

    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    public static double tan(double a) {
        return Math.tan(a);
    }

    public static double tanh(double x) {
        return Math.tanh(x);
    }

    public static double toDegrees(double angrad) {
        return Math.toDegrees(angrad);
    }

    public static double toRadians(double angdeg) {
        return Math.toRadians(angdeg);
    }

    public static double ulp(double d) {
        return Math.ulp(d);
    }

    public static float ulp(float f) {
        return Math.ulp(f);
    }

    public static boolean[] add(boolean[] array, boolean element) {
        return ArrayUtils.add(array, element);
    }

    public static boolean[] add(boolean[] array, int index, boolean element) {
        return ArrayUtils.add(array, index, element);
    }

    public static byte[] add(byte[] array, byte element) {
        return ArrayUtils.add(array, element);
    }

    public static byte[] add(byte[] array, int index, byte element) {
        return ArrayUtils.add(array, index, element);
    }

    public static char[] add(char[] array, char element) {
        return ArrayUtils.add(array, element);
    }

    public static char[] add(char[] array, int index, char element) {
        return ArrayUtils.add(array, index, element);
    }

    public static double[] add(double[] array, double element) {
        return ArrayUtils.add(array, element);
    }

    public static double[] add(double[] array, int index, double element) {
        return ArrayUtils.add(array, index, element);
    }

    public static float[] add(float[] array, float element) {
        return ArrayUtils.add(array, element);
    }

    public static float[] add(float[] array, int index, float element) {
        return ArrayUtils.add(array, index, element);
    }

    public static int[] add(int[] array, int element) {
        return ArrayUtils.add(array, element);
    }

    public static int[] add(int[] array, int index, int element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1L], 0, 2L)           = [2L, 1L]
     * ArrayUtils.add([2L, 6L], 2, 10L)      = [2L, 6L, 10L]
     * ArrayUtils.add([2L, 6L], 0, -4L)      = [-4L, 2L, 6L]
     * ArrayUtils.add([2L, 6L, 3L], 2, 1L)   = [2L, 6L, 1L, 3L]
     * </pre>
     *
     * @param array  the array to add the element to, may be <code>null</code>
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static long[] add(long[] array, int index, long element) {
        return ArrayUtils.add(array, index, element);
    }
    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be <code>null</code>
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static long[] add(long[] array, long element) {
        return ArrayUtils.add(array, element);
    }
    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0, null)      = [null]
     * ArrayUtils.add(null, 0, "a")       = ["a"]
     * ArrayUtils.add(["a"], 1, null)     = ["a", null]
     * ArrayUtils.add(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array  the array to add the element to, may be <code>null</code>
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static Object[] add(Object[] array, int index, Object element) {
        return ArrayUtils.add(array, index, element);
    }
    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element, unless the element itself is null,
     *  in which case the return type is Object[]</p>
     *
     * <pre>
     * ArrayUtils.add(null, null)      = [null]
     * ArrayUtils.add(null, "a")       = ["a"]
     * ArrayUtils.add(["a"], null)     = ["a", null]
     * ArrayUtils.add(["a"], "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array  the array to "add" the element to, may be <code>null</code>
     * @param element  the object to add, may be <code>null</code>
     * @return A new array containing the existing elements plus the new element
     * The returned array type will be that of the input array (unless null),
     * in which case it will have the same type as the element.
     */
    public static Object[] add(Object[] array, Object element) {
        return ArrayUtils.add(array, element);
    }
    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add([1], 0, 2)         = [2, 1]
     * ArrayUtils.add([2, 6], 2, 10)     = [2, 6, 10]
     * ArrayUtils.add([2, 6], 0, -4)     = [-4, 2, 6]
     * ArrayUtils.add([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array  the array to add the element to, may be <code>null</code>
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static short[] add(short[] array, int index, short element) {
        return ArrayUtils.add(array, index, element);
    }
    /**
     * <p>Copies the given array and adds the given element at the end of the new array.</p>
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array  the array to copy and add the element to, may be <code>null</code>
     * @param element  the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static short[] add(short[] array, short element) {
        return ArrayUtils.add(array, element);
    }
    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0, null)      = [null]
     * ArrayUtils.add(null, 0, "a")       = ["a"]
     * ArrayUtils.add(["a"], 1, null)     = ["a", null]
     * ArrayUtils.add(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array  the array to add the element to, may be <code>null</code>
     * @param index  the position of the new object
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static Object[] addIgnoreNull(Object[] array, int index, Object element) {
        if (element != null) {
            return ArrayUtils.add(array, index, element);
        }
        return array;
    }
    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, a new one element array is returned
     *  whose component type is the same as the element.</p>
     *
     * <pre>
     * ArrayUtils.add(null, 0, null)      = [null]
     * ArrayUtils.add(null, 0, "a")       = ["a"]
     * ArrayUtils.add(["a"], 1, null)     = ["a", null]
     * ArrayUtils.add(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array  the array to add the element to, may be <code>null</code>
     * @param element  the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index > array.length).
     */
    public static Object[] addIgnoreNull(Object[] array, Object element) {
        if (element != null) {
            return ArrayUtils.add(array, element);
        }
        return array;
    }

    /**
     * Use {@link #addIgnoreNull(Object[], int, Object)} instead.
     */
    @Deprecated
    public static Object[] addArrayElementIgnoreNull(Object[] array, int index, Object element) {
        return addIgnoreNull(array, index, element);
    }

    /**
     * Use {@link #addIgnoreNull(Object[], Object)} instead.
     */
    @Deprecated
    public static Object[] addArrayElementIgnoreNull(Object[] array, Object element) {
        return addIgnoreNull(array, element);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new boolean[] array.
     */
    public static boolean[] addAll(boolean[] array1, boolean[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new byte[] array.
     */
    public static byte[] addAll(byte[] array1, byte[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new char[] array.
     */
    public static char[] addAll(char[] array1, char[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new double[] array.
     */
    public static double[] addAll(double[] array1, double[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new float[] array.
     */
    public static float[] addAll(float[] array1, float[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new int[] array.
     */
    public static int[] addAll(int[] array1, int[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new long[] array.
     */
    public static long[] addAll(long[] array1, long[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(null, null)     = null
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * ArrayUtils.addAll([null], [null]) = [null, null]
     * ArrayUtils.addAll(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array, may be <code>null</code>
     * @param array2  the second array whose elements are added to the new array, may be <code>null</code>
     * @return The new array, <code>null</code> if both arrays are <code>null</code>.
     *      The type of the new array is the type of the first array,
     *      unless the first array is null, in which case the type is the same as the second array.
     * @throws IllegalArgumentException if the array types are incompatible
     */
    public static Object[] addAll(Object[] array1, Object[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1  the first array whose elements are added to the new array.
     * @param array2  the second array whose elements are added to the new array.
     * @return The new short[] array.
     */
    public static short[] addAll(short[] array1, short[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([true], 0)              = []
     * ArrayUtils.remove([true, false], 0)       = [false]
     * ArrayUtils.remove([true, false], 1)       = [true]
     * ArrayUtils.remove([true, true, false], 1) = [true, false]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static boolean[] remove(boolean[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([true], 0)              = []
     * ArrayUtils.remove([true, false], 0)       = [false]
     * ArrayUtils.remove([true, false], 1)       = [true]
     * ArrayUtils.remove([true, true, false], 1) = [true, false]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static byte[] remove(byte[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove(['a'], 0)           = []
     * ArrayUtils.remove(['a', 'b'], 0)      = ['b']
     * ArrayUtils.remove(['a', 'b'], 1)      = ['a']
     * ArrayUtils.remove(['a', 'b', 'c'], 1) = ['a', 'c']
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static char[] remove(char[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static double[] remove(double[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static float[] remove(float[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static int[] remove(int[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static long[] remove(long[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove(["a"], 0)           = []
     * ArrayUtils.remove(["a", "b"], 0)      = ["b"]
     * ArrayUtils.remove(["a", "b"], 1)      = ["a"]
     * ArrayUtils.remove(["a", "b", "c"], 1) = ["a", "c"]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static Object[] remove(Object[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <p>If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.</p>
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array  the array to remove the element from, may not be <code>null</code>
     * @param index  the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     * (index < 0 || index >= array.length), or if the array is <code>null</code>.
     */
    public static short[] remove(short[] array, int index) {
        return ArrayUtils.remove(array, index);
    }
    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (substracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, true)                = null
     * ArrayUtils.removeElement([], true)                  = []
     * ArrayUtils.removeElement([true], false)             = [true]
     * ArrayUtils.removeElement([true, false], false)      = [true]
     * ArrayUtils.removeElement([true, false, true], true) = [false, true]
     * </pre>
     *
     * @param array  the array to remove the element from, may be <code>null</code>
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static boolean[] removeElement(boolean[] array, boolean element) {
        return ArrayUtils.removeElement(array, element);
    }
    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (substracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)        = null
     * ArrayUtils.removeElement([], 1)          = []
     * ArrayUtils.removeElement([1], 0)         = [1]
     * ArrayUtils.removeElement([1, 0], 0)      = [1]
     * ArrayUtils.removeElement([1, 0, 1], 1)   = [0, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be <code>null</code>
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static byte[] removeElement(byte[] array, byte element) {
        return ArrayUtils.removeElement(array, element);
    }
    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (substracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 'a')            = null
     * ArrayUtils.removeElement([], 'a')              = []
     * ArrayUtils.removeElement(['a'], 'b')           = ['a']
     * ArrayUtils.removeElement(['a', 'b'], 'a')      = ['b']
     * ArrayUtils.removeElement(['a', 'b', 'a'], 'a') = ['b', 'a']
     * </pre>
     *
     * @param array  the array to remove the element from, may be <code>null</code>
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static char[] removeElement(char[] array, char element) {
        return ArrayUtils.removeElement(array, element);
    }
    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (substracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be <code>null</code>
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static double[] removeElement(double[] array, double element) {
        return ArrayUtils.removeElement(array, element);
    }
    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (substracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be <code>null</code>
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */  
    public static float[] removeElement(float[] array, float element) {
        return ArrayUtils.removeElement(array, element);
    }
    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (substracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be <code>null</code>
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static int[] removeElement(int[] array, int element) {
        return ArrayUtils.removeElement(array, element);
    }
    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (substracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.</p>
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.</p>
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array  the array to remove the element from, may be <code>null</code>
     * @param element  the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static long[] removeElement(long[] array, long element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array. <br /><br />
     * 
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array. <br /><br />
     * 
     * <code>
     * ArrayUtils.removeElement(null, "a")            = null        <br />
     * ArrayUtils.removeElement([], "a")              = []          <br />
     * ArrayUtils.removeElement(["a"], "b")           = ["a"]       <br />
     * ArrayUtils.removeElement(["a", "b"], "a")      = ["b"]       <br />
     * ArrayUtils.removeElement(["a", "b", "a"], "a") = ["b", "a"]  <br />
     * </code>
     * 
     * @param array the array to remove the element from, may be null
     * @param element
     * @return the element to be removed 
     */
    public static Object[] removeElement(Object[] array, Object element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static short[] removeElement(short[] array, short element) {
        return ArrayUtils.removeElement(array, element);
    }

    // <<< isEmpty section for arrays and Strings >>>
    /**
     * Checks if an array of Objects is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(Object[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of primitive bytes is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(byte[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of primitive chars is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(char[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of primitive shorts is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(short[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of primitive ints is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(int[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of primitive longs is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(long[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of primitive floats is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(float[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of primitive doubles is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(double[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of Dates is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(Date[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of BigDecimals is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(BigDecimal[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if an array of BigIntegers is empty or null.
     * @param array the array to test 
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(BigInteger[] array) {
        return ArrayUtils.isEmpty(array);
    }
    /**
     * Checks if a String is empty ("") or null.<br /><br />
     * <code>
     * StringUtils.isEmpty(null)      = true <br />
     * StringUtils.isEmpty("")        = true <br />
     * StringUtils.isEmpty(" ")       = false <br />
     * StringUtils.isEmpty("bob")     = false <br />
     * StringUtils.isEmpty("  bob  ") = false <br />
     * </code>
     * @param str the String to check, may be null 
     * @return true if the String is empty or null
     */
    public static boolean isEmpty(String str) {
        return StringUtils.isBlank(str);
    }

    // <<< startsWith and endsWith for Strings >>>
    /**
     * Check if a String starts with a specified prefix.<br /><br />
     * Two null references are considered to be equal. The comparison is case sensitive.<br /><br />
     * <code>
     *  StringUtils.startsWith(null, null)      = true <br />
     * StringUtils.startsWith(null, "abc")     = false <br />
     * StringUtils.startsWith("abcdef", null)  = false <br />
     * StringUtils.startsWith("abcdef", "abc") = true <br />
     * StringUtils.startsWith("ABCDEF", "abc") = false <br />
     * </code>
     * @param str the String to check, may be null
     * @param prefix the prefix to find, may be null 
     * @return true if the String starts with the prefix, case sensitive, or both null
     */
    public static boolean startsWith(String str, String prefix) {
        return StringUtils.startsWith(str, prefix);
    }

    /**
     * Check if a String ends with a specified suffix.<br />
     * <br />
     * Two null references are considered to be equal. The comparison is case
     * sensitive.<br />
     * <br />
     * <code>
     * StringUtils.endsWith(null, null)      = true <br />
     * StringUtils.endsWith(null, "def")     = false <br />
     * StringUtils.endsWith("abcdef", null)  = false <br />
     * StringUtils.endsWith("abcdef", "def") = true <br />
     * StringUtils.endsWith("ABCDEF", "def") = false <br />
     * StringUtils.endsWith("ABCDEF", "cde") = false <br />
     * </code>
     * 
     * @param str the String to check, may be null
     * @param suffix the suffix to find, may be null
     * @return true if the String ends with the suffix, case sensitive, or both
     *         null
     */
    public static boolean endsWith(String str, String suffix) {
        return StringUtils.endsWith(str, suffix);
    }

    // <<< subString >>>
    /**
     * Gets a substring from the specified String<br />
     * <br />
     * A negative start position can be used to start n characters from the end
     * of the String.<br />
     * <br />
     * A null String will return null. An empty ("") String will return "".<br />
     * <br />
     * * StringUtils.substring(null, *) = null <br />
     * StringUtils.substring("", *) = "" <br />
     * StringUtils.substring("abc", 0) = "abc" <br />
     * StringUtils.substring("abc", 2) = "c" <br />
     * StringUtils.substring("abc", 4) = "" <br />
     * StringUtils.substring("abc", -2) = "bc" <br />
     * StringUtils.substring("abc", -4) = "abc" <br />
     * 
     * @param str the String to get the substring from, may be null
     * @param beginIndex the position to start from, negative means count back from the end of the String by this many characters 
     * @return substring from start position, null if null String input
     */
    public static String substring(String str, int beginIndex) {
        return StringUtils.substring(str, beginIndex);
    }

    /**
     * Gets a substring from the specified String <br />
     * <br />
     * A negative start position can be used to start/end n characters from the
     * end of the String. <br />
     * <br />
     * The returned substring starts with the character in the start position
     * and ends before the end position. All position counting is zero-based --
     * i.e., to start at the beginning of the string use start = 0. Negative
     * start and end positions can be used to specify offsets relative to the
     * end of the String. <br />
     * <br />
     * If start is not strictly to the left of end, "" is returned.<br />
     * <br />
     * <code>
     * StringUtils.substring(null, *, *)    = null <br />
     * StringUtils.substring("", * ,  *)    = ""; <br />
     * StringUtils.substring("abc", 0, 2)   = "ab" <br />
     * StringUtils.substring("abc", 2, 0)   = "" <br />
     * StringUtils.substring("abc", 2, 4)   = "c" <br />
     * StringUtils.substring("abc", 4, 6)   = "" <br />
     * StringUtils.substring("abc", 2, 2)   = "" <br />
     * StringUtils.substring("abc", -2, -1) = "b" <br />
     * StringUtils.substring("abc", -4, 2)  = "ab" <br />
     * </code>
     * 
     * @param str the String to get the substring from, may be null
     * @param beginIndex the position to start from, negative means count back
     *            from the end of the String by this many characters
     * @param endIndex the position to end at (exclusive), negative means
     *            count back from the end of the String by this many characters
     * @return substring from start position to end positon, null if null String
     *         input
     */
    public static String substring(String str, int beginIndex, int endIndex) {
        return StringUtils.substring(str, beginIndex, endIndex);
    }

    // <<< removeStart and removeEnd >>>

    /*
     * into the return statement the full path of StringUtils should be written
     * if we write StringUtils.removeStart(str, remove); WebStudio won't work
     * correctly;
     */
    /**
     * Removes a substring only if it is at the begining of a source string,
     * otherwise returns the source string. <br />
     * <br />
     * 
     * A null source string will return null. An empty ("") source string will
     * return the empty string. A null search string will return the source
     * string. <br />
     * <br />
     * <code>
     * StringUtils.removeStart(null, *)      = null <br />
     * StringUtils.removeStart("", *)        = "" <br />
     * StringUtils.removeStart(*, null)      = * <br />
     * StringUtils.removeStart("www.domain.com", "www.")   = "domain.com" <br />
     * StringUtils.removeStart("domain.com", "www.")       = "domain.com" <br />
     * StringUtils.removeStart("www.domain.com", "domain") = "www.domain.com" <br />
     * StringUtils.removeStart("abc", "")    = "abc" <br />
     * </code>
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the substring with the string removed if found, null if null
     *         String input
     */
    public static String removeStart(String str, String remove) {
        return org.apache.commons.lang.StringUtils.removeStart(str, remove);
    }

    /**
     * Removes a substring only if it is at the end of a source string,
     * otherwise returns the source string. <br /> <br />
     * 
     * A null source string will return null. An empty ("") source string will
     * return the empty string. A null search string will return the source
     * string. <br />
     * <br />
     * <code>
     * StringUtils.removeEnd(null, *)      = null <br />
     * StringUtils.removeEnd("", *)        = "" <br />
     * StringUtils.removeEnd(*, null)      = * <br />
     * StringUtils.removeEnd("www.domain.com", ".com.")  = "www.domain.com" <br />
     * StringUtils.removeEnd("www.domain.com", ".com")   = "www.domain" <br />
     * StringUtils.removeEnd("www.domain.com", "domain") = "www.domain.com" <br />
     * StringUtils.removeEnd("abc", "")    = "abc" <br />
     * </code>
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null 
     * @return the String to search for and remove, may be null 
     */
    public static String removeEnd(String str, String remove) {
        return StringUtils.removeEnd(str, remove);
    }

    // <<< lowerCase and upperCase functions >>>
    /**
     * Converts a String to lower case <br /><br />
     * A null input String returns null. <br /><br />
     * 
     * <code>
     * StringUtils.lowerCase(null)  = null <br />
     * StringUtils.lowerCase("")    = "" <br />
     * StringUtils.lowerCase("aBc") = "abc" <br />
     * </code>
     *
     * @param str the String to lower case, may be null 
     * @return the lower cased String, null if null String input
     */
    public static String lowerCase(String str) {
        return StringUtils.lowerCase(str);
    }
    /**
     * Converts a String to upper case <br /><br />
     * A null input String returns null.<br /> <br />
     * 
     * <code>
     * StringUtils.upperCase(null)  = null <br />
     * StringUtils.upperCase("")    = "" <br />
     * StringUtils.upperCase("aBc") = "ABC" <br />
     * </code>
     * 
     * @param str the String to upper case, may be null 
     * @return the upper cased String, null if null String input
     */
    public static String upperCase(String str) {
        return StringUtils.upperCase(str);
    }

    // <<< replace functions for Strings >>>
    /**
     * Replaces all occurrences of a String within another String <br />
     * <br />
     * 
     * A null reference passed to this method is a no-op. <br />
     * <br />
     * 
     * <code>
     * StringUtils.replace(null, *, *)        = null <br />
     *  StringUtils.replace("", *, *)          = "" <br />
     *  StringUtils.replace("any", null, *)    = "any" <br />
     *  StringUtils.replace("any", *, null)    = "any" <br />
     * StringUtils.replace("any", "", *)      = "any" <br />
     * StringUtils.replace("aba", "a", null)  = "aba" <br />
     *  StringUtils.replace("aba", "a", "")    = "b" <br />
     *  StringUtils.replace("aba", "a", "z")   = "zbz" <br />
     * </code>
     * 
     * @param str text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement the String to replace it with, may be null
     * @return the text with any replacements processed, null if null String
     *         input
     */
    public static String replace(String str, String searchString, String replacement) {
        return StringUtils.replace(str, searchString, replacement);
    }

    /**
     * Replaces a String with another String inside a larger String, for the
     * first max values of the search String.<br><br />
     * A null reference passed to this method is a no-op. <br /><br />
     * 
     * <code>
     *  StringUtils.replace(null, *, *, *)         = null <br />
     *  StringUtils.replace("", *, *, *)           = "" <br />
     *  StringUtils.replace("any", null, *, *)     = "any" <br />
     *  StringUtils.replace("any", *, null, *)     = "any"    <br />
     *  StringUtils.replace("any", "", *, *)       = "any"    <br />
     *  StringUtils.replace("any", *, *, 0)        = "any"    <br />
     *  StringUtils.replace("abaa", "a", null, -1) = "abaa" <br />
     *  StringUtils.replace("abaa", "a", "", -1)   = "b" <br />
     *  StringUtils.replace("abaa", "a", "z", 0)   = "abaa" <br />
     *  StringUtils.replace("abaa", "a", "z", 1)   = "zbaa" <br />
     *  StringUtils.replace("abaa", "a", "z", 2)   = "zbza" <br />
     *  StringUtils.replace("abaa", "a", "z", -1)  = "zbzz" <br />
     * </code>
     * 
     * @param str text to search and replace in, may be null
     * @param searchString  the String to search for, may be null
     * @param replacement  the String to replace it with, may be null
     * @param max maximum number of values to replace, or -1 if no maximum
     * @return the text with any replacements processed, null if null String
     *         input
     */
    public static String replace(String str, String searchString, String replacement, int max) {
        return StringUtils.replace(str, searchString, replacement, max);
    }
}
