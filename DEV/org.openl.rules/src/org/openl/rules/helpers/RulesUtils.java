/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.helpers;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.domain.IDomain;
import org.openl.exception.OpenLRuntimeException;
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
import org.openl.types.impl.DomainOpenClass;
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

    /**
     * REturns max Byte value
     *
     * @param values Byte array
     * @return max Byte
     */
    public static java.lang.Byte max(java.lang.Byte[] values) {
        return (java.lang.Byte) MathUtils.max(values);
    }

    /**
     * Returns max short value
     *
     * @param values Short array
     * @return max short
     */
    public static java.lang.Short max(java.lang.Short[] values) {
        return (java.lang.Short) MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Integer max(java.lang.Integer[] values) {
        return (java.lang.Integer) MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Long max(java.lang.Long[] values) {
        return (java.lang.Long) MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Float max(java.lang.Float[] values) {
        return (java.lang.Float) MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Double max(java.lang.Double[] values) {
        return (java.lang.Double) MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.math.BigInteger max(java.math.BigInteger[] values) {
        return (java.math.BigInteger) MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.math.BigDecimal max(java.math.BigDecimal[] values) {
        return (java.math.BigDecimal) MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static byte max(byte[] values) {
        return MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static short max(short[] values) {
        return MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static int max(int[] values) {
        return MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static long max(long[] values) {
        return MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static float max(float[] values) {
        return MathUtils.max(values);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static double max(double[] values) {
        return MathUtils.max(values);
    }

    // MIN

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Byte min(java.lang.Byte[] values) {
        return (java.lang.Byte) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Short min(java.lang.Short[] values) {
        return (java.lang.Short) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Integer min(java.lang.Integer[] values) {
        return (java.lang.Integer) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Long min(java.lang.Long[] values) {
        return (java.lang.Long) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Float min(java.lang.Float[] values) {
        return (java.lang.Float) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.lang.Double min(java.lang.Double[] values) {
        return (java.lang.Double) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.math.BigInteger min(java.math.BigInteger[] values) {
        return (java.math.BigInteger) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static java.math.BigDecimal min(java.math.BigDecimal[] values) {
        return (java.math.BigDecimal) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static byte min(byte[] values) {
        return (byte) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static short min(short[] values) {
        return (short) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static int min(int[] values) {
        return (int) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static long min(long[] values) {
        return (long) MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static float min(float[] values) {
        return MathUtils.min(values);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static double min(double[] values) {
        return MathUtils.min(values);
    }

    // SUM

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Byte sum(java.lang.Byte[] values) {
        return (java.lang.Byte) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Short sum(java.lang.Short[] values) {
        return (java.lang.Short) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Integer sum(java.lang.Integer[] values) {
        return (java.lang.Integer) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Long sum(java.lang.Long[] values) {
        return (java.lang.Long) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Float sum(java.lang.Float[] values) {
        return (java.lang.Float) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Double sum(java.lang.Double[] values) {
        return (java.lang.Double) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.math.BigInteger sum(java.math.BigInteger[] values) {
        return (java.math.BigInteger) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.math.BigDecimal sum(java.math.BigDecimal[] values) {
        return (java.math.BigDecimal) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static byte sum(byte[] values) {
        return (byte) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static short sum(short[] values) {
        return (short) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static int sum(int[] values) {
        return (int) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static long sum(long[] values) {
        return (long) MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static float sum(float[] values) {
        return MathUtils.sum(values);
    }

    /**
     * <p>
     * Returns the sum of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static double sum(double[] values) {
        return MathUtils.sum(values);
    }

    // AVERAGE

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Byte avg(java.lang.Byte[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Short avg(java.lang.Short[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Integer avg(java.lang.Integer[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Long avg(java.lang.Long[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Float avg(java.lang.Float[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.lang.Double avg(java.lang.Double[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.math.BigInteger avg(java.math.BigInteger[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static java.math.BigDecimal avg(java.math.BigDecimal[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static byte avg(byte[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static short avg(short[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static int avg(int[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static long avg(long[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static float avg(float[] values) {
        return MathUtils.avg(values);
    }

    /**
     * <p>
     * Returns the average value of the elements in the array.
     * </p>
     *
     * @param values an array, must not be null or empty
     * @return the sum of the elements in the array
     */
    public static double avg(double[] values) {
        return MathUtils.avg(values);
    }

    // SMALL

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Byte small(java.lang.Byte[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Short small(java.lang.Short[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Integer small(java.lang.Integer[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Long small(java.lang.Long[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Float small(java.lang.Float[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Double small(java.lang.Double[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.math.BigInteger small(java.math.BigInteger[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.math.BigDecimal small(java.math.BigDecimal[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static byte small(byte[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static short small(short[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static int small(int[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static long small(long[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static float small(float[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static double small(double[] values, int position) {
        return MathUtils.small(values, position);
    }

    // BIG

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Byte big(java.lang.Byte[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Short big(java.lang.Short[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Integer big(java.lang.Integer[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Long big(java.lang.Long[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Float big(java.lang.Float[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Double big(java.lang.Double[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.math.BigInteger big(java.math.BigInteger[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.math.BigDecimal big(java.math.BigDecimal[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static byte big(byte[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static short big(short[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static int big(int[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static long big(long[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static float big(float[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and
     * returns the value at position <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static double big(double[] values, int position) {
        return MathUtils.big(values, position);
    }

    // MEDIAN

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Byte median(java.lang.Byte[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Short median(java.lang.Short[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Integer median(java.lang.Integer[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Long median(java.lang.Long[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Float median(java.lang.Float[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Double median(java.lang.Double[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.math.BigInteger median(java.math.BigInteger[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.math.BigDecimal median(java.math.BigDecimal[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static byte median(byte[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static short median(short[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static int median(int[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static long median(long[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static float median(float[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static double median(double[] values) {
        return MathUtils.median(values);
    }

    // QUAOTIENT

    /**
     * Returns the
     *
     * @param number
     * @param divisor
     * @return
     */
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

    /**
     * Sorts the specified array of BigDecimals into ascending order, according
     * to the {@linkplain Comparable natural ordering} of its elements. All
     * elements in the array must implement the {@link Comparable} interface.
     * Furthermore, all elements in the array must be <i>mutually comparable</i>
     * (that is, {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in
     * the array).
     * <p/>
     * <p/>
     * This sort is guaranteed to be <i>stable</i>: equal elements will not be
     * reordered as a result of the sort.
     * <p/>
     * <p/>
     * Implementation note: This implementation is a stable, adaptive, iterative
     * mergesort that requires far fewer than n lg(n) comparisons when the input
     * array is partially sorted, while offering the performance of a
     * traditional mergesort when the input array is randomly ordered. If the
     * input array is nearly sorted, the implementation requires approximately n
     * comparisons. Temporary storage requirements vary from a small constant
     * for nearly sorted input arrays to n/2 object references for randomly
     * ordered input arrays.
     * <p/>
     * <p/>
     * The implementation takes equal advantage of ascending and descending
     * order in its input array, and can take advantage of ascending and
     * descending order in different parts of the the same input array. It is
     * well-suited to merging two or more sorted arrays: simply concatenate the
     * arrays and sort the resulting array.
     * <p/>
     * <p/>
     * The implementation was adapted from Tim Peters's list sort for Python (<a
     * href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>). It uses techiques from Peter McIlroy's "Optimistic Sorting
     * and Information Theoretic Complexity", in Proceedings of the Fourth
     * Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474, January
     * 1993.
     *
     * @param values the array to be sorted
     * @return a sorted array of BigDecimals
     */
    public static java.math.BigDecimal[] sort(java.math.BigDecimal[] values) {
        return MathUtils.sort(values);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on many data sets that cause other
     * quicksorts to degrade to quadratic performance, and is typically faster
     * than traditional (one-pivot) Quicksort implementations.
     *
     * @param values the array to be sorted
     * @return a sorted array of bytes
     */
    public static byte[] sort(byte[] values) {
        return MathUtils.sort(values);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on many data sets that cause other
     * quicksorts to degrade to quadratic performance, and is typically faster
     * than traditional (one-pivot) Quicksort implementations.
     *
     * @param values the array to be sorted
     * @return a sorted array of shorts
     */
    public static short[] sort(short[] values) {
        return MathUtils.sort(values);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on many data sets that cause other
     * quicksorts to degrade to quadratic performance, and is typically faster
     * than traditional (one-pivot) Quicksort implementations.
     *
     * @param values the array to be sorted
     * @return a sorted array of ints
     */
    public static int[] sort(int[] values) {
        return MathUtils.sort(values);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on many data sets that cause other
     * quicksorts to degrade to quadratic performance, and is typically faster
     * than traditional (one-pivot) Quicksort implementations.
     *
     * @param values the array to be sorted
     * @return a sorted array of longs
     */
    public static long[] sort(long[] values) {
        return MathUtils.sort(values);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     * <p/>
     * <p/>
     * The {@code <} relation does not provide a total order on all double
     * values: {@code -0.0d == 0.0d} is {@code true} and a {@code Double.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Double#compareTo}: {@code -0.0d} is treated as less than value
     * {@code 0.0d} and {@code Double.NaN} is considered greater than any other
     * value and all {@code Double.NaN} values are considered equal.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on many data sets that cause other
     * quicksorts to degrade to quadratic performance, and is typically faster
     * than traditional (one-pivot) Quicksort implementations.
     *
     * @param values the array to be sorted
     * @return a sorted array of floats
     */
    public static float[] sort(float[] values) {
        return MathUtils.sort(values);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     * <p/>
     * <p/>
     * The {@code <} relation does not provide a total order on all double
     * values: {@code -0.0d == 0.0d} is {@code true} and a {@code Double.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Double#compareTo}: {@code -0.0d} is treated as less than value
     * {@code 0.0d} and {@code Double.NaN} is considered greater than any other
     * value and all {@code Double.NaN} values are considered equal.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on many data sets that cause other
     * quicksorts to degrade to quadratic performance, and is typically faster
     * than traditional (one-pivot) Quicksort implementations.
     *
     * @param - values the array to be sorted
     * @return a sorted array of doubles
     */
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

    /**
     * <p>
     * Checks if the object is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param obj the object to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(Object[] array, Object obj) {
        return ArrayUtils.contains(array, obj);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(int[] array, int elem) {
        return ArrayUtils.contains(array, elem);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(long[] array, long elem) {
        return ArrayUtils.contains(array, elem);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(byte[] array, byte elem) {
        return ArrayUtils.contains(array, elem);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(short[] array, short elem) {
        return ArrayUtils.contains(array, elem);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem the value to find
     * @return <code>true</code> if the array contains the object
     * @since 2.1
     */
    public static boolean contains(char[] array, char elem) {
        return ArrayUtils.contains(array, elem);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(float[] array, float elem) {
        return ArrayUtils.contains(array, elem);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(double[] array, double elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Byte[] array, Byte elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Short[] array, Short elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Integer[] array, Integer elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Long[] array, Long elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Float[] array, Float elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Double[] array, Double elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Character[] array, Character elem) {
        return ArrayUtils.contains(array, elem);
    }

    // ------------------------------------------------

    public static boolean contains(Object[] ary1, Object[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(boolean[] array, boolean elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Boolean[] array, Boolean elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(int[] ary1, int[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Integer[] ary1, Integer[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(byte[] ary1, byte[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Byte[] ary1, Byte[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(short[] ary1, short[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Short[] ary1, Short[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(long[] ary1, long[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Long[] ary1, Long[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(char[] ary1, char[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Character[] ary1, Character[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(float[] ary1, float[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Float[] ary1, Float[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(String[] ary1, String[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(double[] ary1, double[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Double[] ary1, Double[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(boolean[] ary1, boolean[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    public static boolean contains(Boolean[] ary1, Boolean[] ary2) {
        return ArrayTool.containsAll(ary1, ary2);
    }

    /**
     * <p>
     * Checks if String contains a search String, handling <code>null</code>.
     * This method uses {@link String#indexOf(String)}.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> String will return <code>false</code>.
     * </p>
     * <p/>
     * 
     * <pre>
     * StringUtils.contains(null, *)     = false
     * StringUtils.contains(*, null)     = false
     * StringUtils.contains("", "")      = true
     * StringUtils.contains("abc", "")   = true
     * StringUtils.contains("abc", "a")  = true
     * StringUtils.contains("abc", "z")  = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchStr the String to find, may be null
     * @return true if the String contains the search String, false if not or
     *         <code>null</code> string input
     */
    public static boolean contains(String str, String searchStr) {
        return StringUtils.contains(str, searchStr);
    }

    /**
     * <p>
     * Checks if String contains a search character, handling <code>null</code>.
     * This method uses {@link String#indexOf(int)}.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> or empty ("") String will return <code>false</code>.
     * </p>
     * <p/>
     * 
     * <pre>
     * StringUtils.contains(null, *)    = false
     * StringUtils.contains("", *)      = false
     * StringUtils.contains("abc", 'a') = true
     * StringUtils.contains("abc", 'z') = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchChar the character to find
     * @return true if the String contains the search character, false if not or
     *         <code>null</code> string input
     */
    public static boolean contains(String str, char searchChar) {
        return StringUtils.contains(str, searchChar);
    }

    /**
     * <p>
     * Checks if the String contains any character in the given set of
     * characters.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> String will return <code>false</code>. A
     * <code>null</code> or zero length search array will return
     * <code>false</code>.
     * </p>
     * <p/>
     * 
     * <pre>
     * StringUtils.containsAny(null, *)                = false
     * StringUtils.containsAny("", *)                  = false
     * StringUtils.containsAny(*, null)                = false
     * StringUtils.containsAny(*, [])                  = false
     * StringUtils.containsAny("zzabyycdxx",['z','a']) = true
     * StringUtils.containsAny("zzabyycdxx",['b','y']) = true
     * StringUtils.containsAny("aba", ['z'])           = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param chars the chars to search for, may be null
     * @return the <code>true</code> if any of the chars are found,
     *         <code>false</code> if no match or null input
     */
    public static boolean containsAny(String str, char[] chars) {
        return StringUtils.containsAny(str, chars);
    }

    /**
     * <p>
     * Checks if the String contains any character in the given set of
     * characters.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> String will return <code>false</code>. A
     * <code>null</code> search string will return <code>false</code>.
     * </p>
     * <p/>
     * 
     * <pre>
     * StringUtils.containsAny(null, *)            = false
     * StringUtils.containsAny("", *)              = false
     * StringUtils.containsAny(*, null)            = false
     * StringUtils.containsAny(*, "")              = false
     * StringUtils.containsAny("zzabyycdxx", "za") = true
     * StringUtils.containsAny("zzabyycdxx", "by") = true
     * StringUtils.containsAny("aba","z")          = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchChars the chars to search for, may be null
     * @return the <code>true</code> if any of the chars are found,
     *         <code>false</code> if no match or null input
     */
    public static boolean containsAny(String str, String searchChars) {
        return StringUtils.containsAny(str, searchChars);
    }

    /**
     * <p>
     * Finds the index of the given object in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param obj the object to find, may be <code>null</code>
     * @return the index of the object within the array,
     *         {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not found or
     *         <code>null</code> array input
     */
    public static int indexOf(Object[] array, Object obj) {
        return ArrayUtils.indexOf(array, obj);
    }

    /**
     * <p>
     * Finds the index of the given value in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND}
     *         (<code>-1</code>) if not found or <code>null</code> array input
     */
    public static int indexOf(int[] array, int elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    /**
     * <p>
     * Finds the index of the given value in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND}
     *         (<code>-1</code>) if not found or <code>null</code> array input
     */
    public static int indexOf(long[] array, long elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    /**
     * <p>
     * Finds the index of the given value in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND}
     *         (<code>-1</code>) if not found or <code>null</code> array input
     */
    public static int indexOf(byte[] array, byte elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    /**
     * <p>
     * Finds the index of the given value in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND}
     *         (<code>-1</code>) if not found or <code>null</code> array input
     */
    public static int indexOf(short[] array, short elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    /**
     * <p>
     * Finds the index of the given value in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND}
     *         (<code>-1</code>) if not found or <code>null</code> array input
     * @since 2.1
     */
    public static int indexOf(char[] array, char elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    /**
     * <p>
     * Finds the index of the given value in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND}
     *         (<code>-1</code>) if not found or <code>null</code> array input
     */
    public static int indexOf(float[] array, float elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    /**
     * <p>
     * Finds the index of the given value in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a
     * <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be
     *            <code>null</code>
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND}
     *         (<code>-1</code>) if not found or <code>null</code> array input
     */
    public static int indexOf(double[] array, double elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(boolean[] array, boolean elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Boolean[] array, Boolean elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Byte[] array, Byte elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Short[] array, Short elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Long[] array, Long elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Integer[] array, Integer elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Float[] array, Float elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Double[] array, Double elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    public static int indexOf(Character[] array, Character elem) {
        return ArrayUtils.indexOf(array, elem);
    }

    // --------------------------------------

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
     *
     * @param date
     * @return formated date value
     */
    @Deprecated
    public static String format(Date date) {
        return dateToString(date);
    }

    /**
     * method dateToString (Date date, String format) should be used
     *
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
        String stringDate = "Incorrect date format";
        try {
            stringDate = DateTool.dateToString(date, dateFormat);
        } catch (Exception e) {
            throw new OpenLRuntimeException(stringDate + " '" + dateFormat + "'");
        }
        return stringDate;
    }

    /**
     * converts a date to the String according dateFormat
     *
     * @param date date to format
     * @return String formated date value
     * @see DateTool#dateToString;
     */
    public static String dateToString(Date date) {
        String stringDate = "Incorrect date format";
        try {
            stringDate = DateTool.dateToString(date);
        } catch (Exception e) {
            throw new OpenLRuntimeException(stringDate);
        }
        return stringDate;
    }

    public static Date stringToDate(String value) throws ParseException {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);

        return df.parse(value);
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
     * <p/>
     * Shouldn`t be used.
     */
    @Deprecated
    public static double parseFormattedDouble(String s) throws ParseException {
        return parseFormattedDouble(s, DEFAULT_DOUBLE_FORMAT);
    }

    /**
     * Parse the represented string value to the double. Uses default Locale for
     * it. See {@link DecimalFormat#DecimalFormat(String)}
     * <p/>
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

    /**
     * Returns AM or PM
     *
     * @param d Date
     * @return AM or PM
     */
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

    /**
     * Returns the closest {@code long} to the argument, with ties rounding up.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is 0.
     * <li>If the argument is negative infinity or any value less than or equal
     * to the value of {@code Long.MIN_VALUE}, the result is equal to the value
     * of {@code Long.MIN_VALUE}.
     * <li>If the argument is positive infinity or any value greater than or
     * equal to the value of {@code Long.MAX_VALUE}, the result is equal to the
     * value of {@code Long.MAX_VALUE}.
     * </ul>
     *
     * @param value a floating-point value to be rounded to a {@code long}.
     * @return the value of the argument rounded to the nearest {@code long}
     *         value.
     */
    public static long round(double value) {
        return Math.round(value);
    }

    /**
     * Returns the closest {@code int} to the argument, with ties rounding up.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is 0.
     * <li>If the argument is negative infinity or any value less than or equal
     * to the value of {@code Integer.MIN_VALUE}, the result is equal to the
     * value of {@code Integer.MIN_VALUE}.
     * <li>If the argument is positive infinity or any value greater than or
     * equal to the value of {@code Integer.MAX_VALUE}, the result is equal to
     * the value of {@code Integer.MAX_VALUE}.
     * </ul>
     *
     * @param value a floating-point value to be rounded to an integer.
     * @return the value of the argument rounded to the nearest {@code int}
     *         value.
     */
    public static int round(float value) {
        return Math.round(value);
    }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     */
    public static double round(double value, int scale) {
        return MathUtils.round(value, scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounding using the {@link BigDecimal#ROUND_HALF_UP} method.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @return the rounded value.
     */
    public static float round(float value, int scale) {
        return MathUtils.round(value, scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounded using the given method which is any method defined in
     * {@link BigDecimal}.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMethod the rounding method as defined in
     *            {@link BigDecimal}.
     * @return the rounded value.
     */
    public static double round(double value, int scale, int roundingMethod) {
        return MathUtils.round(value, scale, roundingMethod);
    }

    /**
     * Round the given value to the specified number of decimal places. The
     * value is rounded using the given method which is any method defined in
     * {@link BigDecimal}.
     *
     * @param value the value to round.
     * @param scale the number of digits to the right of the decimal point.
     * @param roundingMethod the rounding method as defined in
     *            {@link BigDecimal}.
     * @return the rounded value.
     */
    public static float round(float value, int scale, int roundingMethod) {
        return MathUtils.round(value, scale, roundingMethod);
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

    /**
     * Returns a {@code BigDecimal} whose scale is the specified value, and
     * whose unscaled value is determined by multiplying or dividing this
     * {@code BigDecimal}'s unscaled value by the appropriate power of ten to
     * maintain its overall value. If the scale is reduced by the operation, the
     * unscaled value must be divided (rather than multiplied), and the value
     * may be changed; in this case, the specified rounding mode is applied to
     * the division.
     * <p/>
     * <p/>
     * Note that since BigDecimal objects are immutable, calls of this method do
     * <i>not</i> result in the original object being modified, contrary to the
     * usual convention of having methods named <tt>set<i>X</i></tt> mutate
     * field <i>{@code X}</i>. Instead, {@code setScale} returns an object with
     * the proper scale; the returned object may or may not be newly allocated.
     * <p/>
     * <p/>
     * The new {@link BigDecimal#setScale(int, RoundingMode)} method should be used in
     * preference to this legacy method.
     *
     * @param scale scale of the {@code BigDecimal} value to be returned.
     * @param roundingMethod The rounding mode to apply.
     * @return a {@code BigDecimal} whose scale is the specified value, and
     *         whose unscaled value is determined by multiplying or dividing
     *         this {@code BigDecimal}'s unscaled value by the appropriate power
     *         of ten to maintain its overall value.
     */
    // added for BA`s, who don`t know about the possibilities of
    // BigDecimal
    public static BigDecimal round(BigDecimal value, int scale, int roundingMethod) {
        if (value == null) {
            return null;
        }

        return value.setScale(scale, roundingMethod);
    }

    /**
     * Return a new array without null elements
     *
     * @param array whose null elements should be removed
     * @return new array without null elements
     */
    public static <T> T[] removeNulls(T[] array) {
        return ArrayTool.removeNulls(array);
    }

    /**
     * Returns the absolute value of a {@code double} value. If the argument is
     * not negative, the argument is returned. If the argument is negative, the
     * negation of the argument is returned. Special cases:
     * <ul>
     * <li>If the argument is positive zero or negative zero, the result is
     * positive zero.
     * <li>If the argument is infinite, the result is positive infinity.
     * <li>If the argument is NaN, the result is NaN.
     * </ul>
     * In other words, the result is the same as the value of the expression:
     * <p/>
     * {@code Double.longBitsToDouble((Double.doubleToLongBits(a)<<1)>>>1)}
     *
     * @param a the argument whose absolute value is to be determined
     * @return the absolute value of the argument.
     */
    // Delegation Methods from java.lang.Math class
    public static double abs(double a) {
        return Math.abs(a);
    }

    /**
     * Returns the absolute value of a {@code float} value. If the argument is
     * not negative, the argument is returned. If the argument is negative, the
     * negation of the argument is returned. Special cases:
     * <ul>
     * <li>If the argument is positive zero or negative zero, the result is
     * positive zero.
     * <li>If the argument is infinite, the result is positive infinity.
     * <li>If the argument is NaN, the result is NaN.
     * </ul>
     * In other words, the result is the same as the value of the expression:
     * <p/>
     * {@code Float.intBitsToFloat(0x7fffffff & Float.floatToIntBits(a))}
     *
     * @param a the argument whose absolute value is to be determined
     * @return the absolute value of the argument.
     */
    public static float abs(float a) {
        return Math.abs(a);
    }

    /**
     * Returns the absolute value of an {@code int} value. If the argument is
     * not negative, the argument is returned. If the argument is negative, the
     * negation of the argument is returned.
     * <p/>
     * <p/>
     * Note that if the argument is equal to the value of
     * {@link Integer#MIN_VALUE}, the most negative representable {@code int}
     * value, the result is that same value, which is negative.
     *
     * @param a the argument whose absolute value is to be determined
     * @return the absolute value of the argument.
     */
    public static int abs(int a) {
        return Math.abs(a);
    }

    /**
     * Returns the absolute value of a {@code long} value. If the argument is
     * not negative, the argument is returned. If the argument is negative, the
     * negation of the argument is returned.
     * <p/>
     * <p/>
     * Note that if the argument is equal to the value of {@link Long#MIN_VALUE}
     * , the most negative representable {@code long} value, the result is that
     * same value, which is negative.
     *
     * @param a the argument whose absolute value is to be determined
     * @return the absolute value of the argument.
     */
    public static long abs(long a) {
        return Math.abs(a);
    }

    /**
     * Returns the arc cosine of a value; the returned angle is in the range 0.0
     * through <i>pi</i>. Special case:
     * <ul>
     * <li>If the argument is NaN or its absolute value is greater than 1, then
     * the result is NaN.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a the value whose arc cosine is to be returned.
     * @return the arc cosine of the argument.
     */
    public static double acos(double a) {
        return Math.acos(a);
    }

    /**
     * Returns the arc sine of a value; the returned angle is in the range
     * -<i>pi</i>/2 through <i>pi</i>/2. Special cases:
     * <ul>
     * <li>If the argument is NaN or its absolute value is greater than 1, then
     * the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a the value whose arc sine is to be returned.
     * @return the arc sine of the argument.
     */
    public static double asin(double a) {
        return Math.asin(a);
    }

    /**
     * Returns the arc tangent of a value; the returned angle is in the range
     * -<i>pi</i>/2 through <i>pi</i>/2. Special cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a the value whose arc tangent is to be returned.
     * @return the arc tangent of the argument.
     */
    public static double atan(double a) {
        return Math.atan(a);
    }

    /**
     * Returns the angle <i>theta</i> from the conversion of rectangular
     * coordinates ({@code x},&nbsp;{@code y}) to polar coordinates
     * (r,&nbsp;<i>theta</i>). This method computes the phase <i>theta</i> by
     * computing an arc tangent of {@code y/x} in the range of -<i>pi</i> to
     * <i>pi</i>. Special cases:
     * <ul>
     * <li>If either argument is NaN, then the result is NaN.
     * <li>If the first argument is positive zero and the second argument is
     * positive, or the first argument is positive and finite and the second
     * argument is positive infinity, then the result is positive zero.
     * <li>If the first argument is negative zero and the second argument is
     * positive, or the first argument is negative and finite and the second
     * argument is positive infinity, then the result is negative zero.
     * <li>If the first argument is positive zero and the second argument is
     * negative, or the first argument is positive and finite and the second
     * argument is negative infinity, then the result is the {@code double}
     * value closest to <i>pi</i>.
     * <li>If the first argument is negative zero and the second argument is
     * negative, or the first argument is negative and finite and the second
     * argument is negative infinity, then the result is the {@code double}
     * value closest to -<i>pi</i>.
     * <li>If the first argument is positive and the second argument is positive
     * zero or negative zero, or the first argument is positive infinity and the
     * second argument is finite, then the result is the {@code double} value
     * closest to <i>pi</i>/2.
     * <li>If the first argument is negative and the second argument is positive
     * zero or negative zero, or the first argument is negative infinity and the
     * second argument is finite, then the result is the {@code double} value
     * closest to -<i>pi</i>/2.
     * <li>If both arguments are positive infinity, then the result is the
     * {@code double} value closest to <i>pi</i>/4.
     * <li>If the first argument is positive infinity and the second argument is
     * negative infinity, then the result is the {@code double} value closest to
     * 3*<i>pi</i>/4.
     * <li>If the first argument is negative infinity and the second argument is
     * positive infinity, then the result is the {@code double} value closest to
     * -<i>pi</i>/4.
     * <li>If both arguments are negative infinity, then the result is the
     * {@code double} value closest to -3*<i>pi</i>/4.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 2 ulps of the exact result. Results
     * must be semi-monotonic.
     *
     * @param y the ordinate coordinate
     * @param x the abscissa coordinate
     * @return the <i>theta</i> component of the point
     *         (<i>r</i>,&nbsp;<i>theta</i>) in polar coordinates that
     *         corresponds to the point (<i>x</i>,&nbsp;<i>y</i>) in Cartesian
     *         coordinates.
     */
    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    /**
     * Returns the cube root of a {@code double} value. For positive finite
     * {@code x}, {@code cbrt(-x) ==
     * -cbrt(x)}; that is, the cube root of a negative value is the negative of
     * the cube root of that value's magnitude.
     * <p/>
     * Special cases:
     * <p/>
     * <ul>
     * <p/>
     * <li>If the argument is NaN, then the result is NaN.
     * <p/>
     * <li>If the argument is infinite, then the result is an infinity with the
     * same sign as the argument.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result.
     *
     * @param a a value.
     * @return the cube root of {@code a}.
     */
    public static double cbrt(double a) {
        return Math.cbrt(a);
    }

    /**
     * Returns the smallest (closest to negative infinity) {@code double} value
     * that is greater than or equal to the argument and is equal to a
     * mathematical integer. Special cases:
     * <ul>
     * <li>If the argument value is already equal to a mathematical integer,
     * then the result is the same as the argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative
     * zero, then the result is the same as the argument.
     * <li>If the argument value is less than zero but greater than -1.0, then
     * the result is negative zero.
     * </ul>
     * Note that the value of {@code Math.ceil(x)} is exactly the value of
     * {@code -Math.floor(-x)}.
     *
     * @param a a value.
     * @return the smallest (closest to negative infinity) floating-point value
     *         that is greater than or equal to the argument and is equal to a
     *         mathematical integer.
     */
    public static double ceil(double a) {
        return Math.ceil(a);
    }

    /**
     * Returns the first floating-point argument with the sign of the second
     * floating-point argument. Note that unlike the
     * {@link StrictMath#copySign(double, double) StrictMath.copySign} method,
     * this method does not require NaN {@code sign} arguments to be treated as
     * positive values; implementations are permitted to treat some NaN
     * arguments as positive and other NaN arguments as negative to allow
     * greater performance.
     *
     * @param magnitude the parameter providing the magnitude of the result
     * @param sign the parameter providing the sign of the result
     * @return a value with the magnitude of {@code magnitude} and the sign of
     *         {@code sign}.
     */
    public static double copySign(double magnitude, double sign) {
        return Math.copySign(magnitude, sign);
    }

    /**
     * Returns the first floating-point argument with the sign of the second
     * floating-point argument. Note that unlike the
     * {@link StrictMath#copySign(float, float) StrictMath.copySign} method,
     * this method does not require NaN {@code sign} arguments to be treated as
     * positive values; implementations are permitted to treat some NaN
     * arguments as positive and other NaN arguments as negative to allow
     * greater performance.
     *
     * @param magnitude the parameter providing the magnitude of the result
     * @param sign the parameter providing the sign of the result
     * @return a value with the magnitude of {@code magnitude} and the sign of
     *         {@code sign}.
     */
    public static float copySign(float magnitude, float sign) {
        return Math.copySign(magnitude, sign);
    }

    /**
     * Returns the trigonometric cosine of an angle. Special cases:
     * <ul>
     * <li>If the argument is NaN or an infinity, then the result is NaN.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a an angle, in radians.
     * @return the cosine of the argument.
     */
    public static double cos(double a) {
        return Math.cos(a);
    }

    /**
     * Returns the hyperbolic cosine of a {@code double} value. The hyperbolic
     * cosine of <i>x</i> is defined to be
     * (<i>e<sup>x</sup>&nbsp;+&nbsp;e<sup>-x</sup></i>)/2 where <i>e</i> is
     * {@linkplain Math#E Euler's number}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <p/>
     * <li>If the argument is NaN, then the result is NaN.
     * <p/>
     * <li>If the argument is infinite, then the result is positive infinity.
     * <p/>
     * <li>If the argument is zero, then the result is {@code 1.0}.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 2.5 ulps of the exact result.
     *
     * @param x The number whose hyperbolic cosine is to be returned.
     * @return The hyperbolic cosine of {@code x}.
     * @since 1.5
     */
    public static double cosh(double x) {
        return Math.cosh(x);
    }

    /**
     * Returns Euler's number <i>e</i> raised to the power of a {@code double}
     * value. Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive
     * infinity.
     * <li>If the argument is negative infinity, then the result is positive
     * zero.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a the exponent to raise <i>e</i> to.
     * @return the value <i>e</i><sup>{@code a}</sup>, where <i>e</i> is the
     *         base of the natural logarithms.
     */
    public static double exp(double a) {
        return Math.exp(a);
    }

    /**
     * Returns <i>e</i><sup>x</sup>&nbsp;-1. Note that for values of <i>x</i>
     * near 0, the exact sum of {@code expm1(x)}&nbsp;+&nbsp;1 is much closer to
     * the true result of <i>e</i><sup>x</sup> than {@code exp(x)}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, then the result is positive
     * infinity.
     * <p/>
     * <li>If the argument is negative infinity, then the result is -1.0.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic. The result of {@code expm1} for any finite input
     * must be greater than or equal to {@code -1.0}. Note that once the exact
     * result of <i>e</i><sup>{@code x}</sup>&nbsp;-&nbsp;1 is within 1/2 ulp of
     * the limit value -1, {@code -1.0} should be returned.
     *
     * @param x the exponent to raise <i>e</i> to in the computation of
     *            <i>e</i><sup>{@code x}</sup>&nbsp;-1.
     * @return the value <i>e</i><sup>{@code x}</sup>&nbsp;-&nbsp;1.
     */
    public static double expm1(double x) {
        return Math.expm1(x);
    }

    /**
     * Returns the largest (closest to positive infinity) {@code double} value
     * that is less than or equal to the argument and is equal to a mathematical
     * integer. Special cases:
     * <ul>
     * <li>If the argument value is already equal to a mathematical integer,
     * then the result is the same as the argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative
     * zero, then the result is the same as the argument.
     * </ul>
     *
     * @param a a value.
     * @return the largest (closest to positive infinity) floating-point value
     *         that less than or equal to the argument and is equal to a
     *         mathematical integer.
     */
    public static double floor(double a) {
        return Math.floor(a);
    }

    /**
     * Returns the unbiased exponent used in the representation of a
     * {@code double}. Special cases:
     * <p/>
     * <ul>
     * <li>If the argument is NaN or infinite, then the result is
     * {@link Double#MAX_EXPONENT} + 1.
     * <li>If the argument is zero or subnormal, then the result is
     * {@link Double#MIN_EXPONENT} -1.
     * </ul>
     *
     * @param d a {@code double} value
     * @return the unbiased exponent of the argument
     */
    public static int getExponent(double d) {
        return Math.getExponent(d);
    }

    /**
     * Returns the unbiased exponent used in the representation of a
     * {@code float}. Special cases:
     * <p/>
     * <ul>
     * <li>If the argument is NaN or infinite, then the result is
     * {@link Float#MAX_EXPONENT} + 1.
     * <li>If the argument is zero or subnormal, then the result is
     * {@link Float#MIN_EXPONENT} -1.
     * </ul>
     *
     * @param f a {@code float} value
     * @return the unbiased exponent of the argument
     */
    public static int getExponent(float f) {
        return Math.getExponent(f);
    }

    /**
     * Returns sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>) without
     * intermediate overflow or underflow.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <p/>
     * <li>If either argument is infinite, then the result is positive infinity.
     * <p/>
     * <li>If either argument is NaN and neither argument is infinite, then the
     * result is NaN.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. If one
     * parameter is held constant, the results must be semi-monotonic in the
     * other parameter.
     *
     * @param x a value
     * @param y a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>) without
     *         intermediate overflow or underflow
     */
    public static double getExponent(double x, double y) {
        return Math.hypot(x, y);
    }

    /**
     * Computes the remainder operation on two arguments as prescribed by the
     * IEEE 754 standard. The remainder value is mathematically equal to
     * <code>f1&nbsp;-&nbsp;f2</code>&nbsp;&times;&nbsp;<i>n</i>, where <i>n</i>
     * is the mathematical integer closest to the exact mathematical value of
     * the quotient {@code f1/f2}, and if two mathematical integers are equally
     * close to {@code f1/f2}, then <i>n</i> is the integer that is even. If the
     * remainder is zero, its sign is the same as the sign of the first
     * argument. Special cases:
     * <ul>
     * <li>If either argument is NaN, or the first argument is infinite, or the
     * second argument is positive zero or negative zero, then the result is
     * NaN.
     * <li>If the first argument is finite and the second argument is infinite,
     * then the result is the same as the first argument.
     * </ul>
     *
     * @param f1 the dividend.
     * @param f2 the divisor.
     * @return the remainder when {@code f1} is divided by {@code f2}.
     */
    public static double IEEEremainder(double f1, double f2) {
        return Math.IEEEremainder(f1, f2);
    }

    /**
     * Returns the natural logarithm (base <i>e</i>) of a {@code double} value.
     * Special cases:
     * <ul>
     * <li>If the argument is NaN or less than zero, then the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive
     * infinity.
     * <li>If the argument is positive zero or negative zero, then the result is
     * negative infinity.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a a value
     * @return the value ln&nbsp;{@code a}, the natural logarithm of {@code a}.
     */
    public static double log(double a) {
        return Math.log(a);
    }

    /**
     * Returns the base 10 logarithm of a {@code double} value. Special cases:
     * <p/>
     * <ul>
     * <li>If the argument is NaN or less than zero, then the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive
     * infinity.
     * <li>If the argument is positive zero or negative zero, then the result is
     * negative infinity.
     * <li>If the argument is equal to 10<sup><i>n</i></sup> for integer
     * <i>n</i>, then the result is <i>n</i>.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a a value
     * @return the base 10 logarithm of {@code a}.
     */
    public static double log10(double a) {
        return Math.log10(a);
    }

    /**
     * Returns the natural logarithm of the sum of the argument and 1. Note that
     * for small values {@code x}, the result of {@code log1p(x)} is much closer
     * to the true result of ln(1 + {@code x}) than the floating-point
     * evaluation of {@code log(1.0+x)}.
     * <p/>
     * <p/>
     * Special cases:
     * <p/>
     * <ul>
     * <p/>
     * <li>If the argument is NaN or less than -1, then the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, then the result is positive
     * infinity.
     * <p/>
     * <li>If the argument is negative one, then the result is negative
     * infinity.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param x a value
     * @return the value ln({@code x}&nbsp;+&nbsp;1), the natural log of
     *         {@code x}&nbsp;+&nbsp;1
     */
    public static double log1p(double x) {
        return Math.log1p(x);
    }

    /**
     * Returns the greater of two {@code int} values. That is, the result is the
     * argument closer to the value of {@link Byte#MAX_VALUE}. If the arguments
     * have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the larger of {@code a} and {@code b}.
     */
    public static Byte max(Byte a, Byte b) {
        return a == null ? b : (b == null ? a : (byte) Math.max(a, b));
    }

    /**
     * Returns the greater of two {@code int} values. That is, the result is the
     * argument closer to the value of {@link Short#MAX_VALUE}. If the arguments
     * have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the larger of {@code a} and {@code b}.
     */
    public static Short max(Short a, Short b) {
        return a == null ? b : (b == null ? a : (short) Math.max(a, b));
    }

    /**
     * Returns the greater of two {@code int} values. That is, the result is the
     * argument closer to the value of {@link Integer#MAX_VALUE}. If the
     * arguments have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the larger of {@code a} and {@code b}.
     */
    public static Integer max(Integer a, Integer b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    /**
     * Returns the greater of two {@code double} values. That is, the result is
     * the argument closer to positive infinity. If the arguments have the same
     * value, the result is that same value. If either value is NaN, then the
     * result is NaN. Unlike the numerical comparison operators, this method
     * considers negative zero to be strictly smaller than positive zero. If one
     * argument is positive zero and the other negative zero, the result is
     * positive zero.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the larger of {@code a} and {@code b}.
     */
    public static Double max(Double a, Double b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    /**
     * Returns the greater of two {@code float} values. That is, the result is
     * the argument closer to positive infinity. If the arguments have the same
     * value, the result is that same value. If either value is NaN, then the
     * result is NaN. Unlike the numerical comparison operators, this method
     * considers negative zero to be strictly smaller than positive zero. If one
     * argument is positive zero and the other negative zero, the result is
     * positive zero.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the larger of {@code a} and {@code b}.
     */
    public static Float max(Float a, Float b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    /**
     * Returns the greater of two {@code long} values. That is, the result is
     * the argument closer to the value of {@link Long#MAX_VALUE}. If the
     * arguments have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the larger of {@code a} and {@code b}.
     */
    public static Long max(Long a, Long b) {
        return a == null ? b : (b == null ? a : Math.max(a, b));
    }

    /**
     * Returns the maximum of this BigInteger and {@code val}.
     *
     * @param val value with which the maximum is to be computed.
     * @return the BigInteger whose value is the greater of this and {@code val}
     *         . If they are equal, either may be returned.
     */
    public static java.math.BigInteger max(java.math.BigInteger a, java.math.BigInteger b) {
        return a == null ? b : (b == null ? a : a.max(b));
    }

    /**
     * Returns the maximum of this {@code BigDecimal} and {@code val}.
     *
     * @param val value with which the maximum is to be computed.
     * @return the {@code BigDecimal} whose value is the greater of this
     *         {@code BigDecimal} and {@code val}. If they are equal, as defined
     *         by the {@link #compareTo(BigDecimal) compareTo} method,
     *         {@code this} is returned.
     * @see #compareTo(java.math.BigDecimal)
     */
    public static java.math.BigDecimal max(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b : (b == null ? a : a.max(b));
    }

    /**
     * Returns the smaller of two {@code int} values. That is, the result the
     * argument closer to the value of {@link Byte#MIN_VALUE}. If the arguments
     * have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the smaller of {@code a} and {@code b}.
     */
    public static Byte min(Byte a, Byte b) {
        return a == null ? b : (b == null ? a : (byte) Math.min(a, b));
    }

    /**
     * Returns the smaller of two {@code int} values. That is, the result the
     * argument closer to the value of {@link Byte#MIN_VALUE}. If the arguments
     * have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the smaller of {@code a} and {@code b}.
     */
    public static Short min(Short a, Short b) {
        return a == null ? b : (b == null ? a : (short) Math.min(a, b));
    }

    /**
     * Returns the smaller of two {@code int} values. That is, the result the
     * argument closer to the value of {@link Integer#MIN_VALUE}. If the
     * arguments have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the smaller of {@code a} and {@code b}.
     */
    public static Integer min(Integer a, Integer b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    /**
     * Returns the smaller of two {@code double} values. That is, the result is
     * the value closer to negative infinity. If the arguments have the same
     * value, the result is that same value. If either value is NaN, then the
     * result is NaN. Unlike the numerical comparison operators, this method
     * considers negative zero to be strictly smaller than positive zero. If one
     * argument is positive zero and the other is negative zero, the result is
     * negative zero.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the smaller of {@code a} and {@code b}.
     */
    public static Double min(Double a, Double b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    /**
     * Returns the smaller of two {@code float} values. That is, the result is
     * the value closer to negative infinity. If the arguments have the same
     * value, the result is that same value. If either value is NaN, then the
     * result is NaN. Unlike the numerical comparison operators, this method
     * considers negative zero to be strictly smaller than positive zero. If one
     * argument is positive zero and the other is negative zero, the result is
     * negative zero.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the smaller of {@code a} and {@code b}.
     */
    public static Float min(Float a, Float b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    /**
     * Returns the smaller of two {@code long} values. That is, the result is
     * the argument closer to the value of {@link Long#MIN_VALUE}. If the
     * arguments have the same value, the result is that same value.
     *
     * @param a an argument.
     * @param b another argument.
     * @return the smaller of {@code a} and {@code b}.
     */
    public static Long min(Long a, Long b) {
        return a == null ? b : (b == null ? a : Math.min(a, b));
    }

    /**
     * Returns the minimum of this BigInteger and {@code val}.
     *
     * @param val value with which the minimum is to be computed.
     * @return the BigInteger whose value is the lesser of this BigInteger and
     */
    public static java.math.BigInteger min(java.math.BigInteger a, java.math.BigInteger b) {
        return a == null ? b : (b == null ? a : a.min(b));
    }

    /**
     * Returns the minimum of this {@code BigDecimal} and {@code val}.
     *
     * @param val value with which the minimum is to be computed.
     * @return the {@code BigDecimal} whose value is the lesser of this
     *         {@code BigDecimal} and {@code val}. If they are equal, as defined
     *         by the {@link #compareTo(BigDecimal) compareTo} method,
     *         {@code this} is returned.
     * @see #compareTo(java.math.BigDecimal)
     */
    public static java.math.BigDecimal min(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b : (b == null ? a : a.min(b));
    }

    /**
     * Returns the floating-point number adjacent to the first argument in the
     * direction of the second argument. If both arguments compare as equal the
     * second argument is returned.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If either argument is a NaN, then NaN is returned.
     * <p/>
     * <li>If both arguments are signed zeros, {@code direction} is returned
     * unchanged (as implied by the requirement of returning the second argument
     * if the arguments compare as equal).
     * <p/>
     * <li>If {@code start} is &plusmn;{@link Double#MIN_VALUE} and
     * {@code direction} has a value such that the result should have a smaller
     * magnitude, then a zero with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is infinite and {@code direction} has a value such
     * that the result should have a smaller magnitude, {@link Double#MAX_VALUE}
     * with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is equal to &plusmn; {@link Double#MAX_VALUE} and
     * {@code direction} has a value such that the result should have a larger
     * magnitude, an infinity with same sign as {@code start} is returned.
     * </ul>
     *
     * @param start starting floating-point value
     * @param direction value indicating which of {@code start}'s neighbors or
     *            {@code start} should be returned
     * @return The floating-point number adjacent to {@code start} in the
     *         direction of {@code direction}.
     */
    public static double nextAfter(double start, double direction) {
        return Math.nextAfter(start, direction);
    }

    /**
     * Returns the floating-point number adjacent to the first argument in the
     * direction of the second argument. If both arguments compare as equal a
     * value equivalent to the second argument is returned.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If either argument is a NaN, then NaN is returned.
     * <p/>
     * <li>If both arguments are signed zeros, a value equivalent to
     * {@code direction} is returned.
     * <p/>
     * <li>If {@code start} is &plusmn;{@link Float#MIN_VALUE} and
     * {@code direction} has a value such that the result should have a smaller
     * magnitude, then a zero with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is infinite and {@code direction} has a value such
     * that the result should have a smaller magnitude, {@link Float#MAX_VALUE}
     * with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is equal to &plusmn; {@link Float#MAX_VALUE} and
     * {@code direction} has a value such that the result should have a larger
     * magnitude, an infinity with same sign as {@code start} is returned.
     * </ul>
     *
     * @param start starting floating-point value
     * @param direction value indicating which of {@code start}'s neighbors or
     *            {@code start} should be returned
     * @return The floating-point number adjacent to {@code start} in the
     *         direction of {@code direction}.
     */
    public static float nextAfter(float start, float direction) {
        return Math.nextAfter(start, direction);
    }

    /**
     * Returns the floating-point value adjacent to {@code f} in the direction
     * of positive infinity. This method is semantically equivalent to
     * {@code nextAfter(f,
     * Float.POSITIVE_INFINITY)}; however, a {@code nextUp} implementation may
     * run faster than its equivalent {@code nextAfter} call.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, the result is positive
     * infinity.
     * <p/>
     * <li>If the argument is zero, the result is {@link Float#MIN_VALUE}
     * <p/>
     * </ul>
     *
     * @param f starting floating-point value
     * @return The adjacent floating-point value closer to positive infinity.
     */
    public static float nextAfter(float f) {
        return Math.nextUp(f);
    }

    /**
     * Returns the floating-point value adjacent to {@code d} in the direction
     * of positive infinity. This method is semantically equivalent to
     * {@code nextAfter(d,
     * Double.POSITIVE_INFINITY)}; however, a {@code nextUp} implementation may
     * run faster than its equivalent {@code nextAfter} call.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, the result is positive
     * infinity.
     * <p/>
     * <li>If the argument is zero, the result is {@link Double#MIN_VALUE}
     * <p/>
     * </ul>
     *
     * @param d starting floating-point value
     * @return The adjacent floating-point value closer to positive infinity.
     */
    public static double nextAfter(double d) {
        return Math.nextUp(d);
    }

    /**
     * Returns the value of the first argument raised to the power of the second
     * argument. Special cases:
     * <p/>
     * <ul>
     * <li>If the second argument is positive or negative zero, then the result
     * is 1.0.
     * <li>If the second argument is 1.0, then the result is the same as the
     * first argument.
     * <li>If the second argument is NaN, then the result is NaN.
     * <li>If the first argument is NaN and the second argument is nonzero, then
     * the result is NaN.
     * <p/>
     * <li>If
     * <ul>
     * <li>the absolute value of the first argument is greater than 1 and the
     * second argument is positive infinity, or
     * <li>the absolute value of the first argument is less than 1 and the
     * second argument is negative infinity,
     * </ul>
     * then the result is positive infinity.
     * <p/>
     * <li>If
     * <ul>
     * <li>the absolute value of the first argument is greater than 1 and the
     * second argument is negative infinity, or
     * <li>the absolute value of the first argument is less than 1 and the
     * second argument is positive infinity,
     * </ul>
     * then the result is positive zero.
     * <p/>
     * <li>If the absolute value of the first argument equals 1 and the second
     * argument is infinite, then the result is NaN.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is positive zero and the second argument is
     * greater than zero, or
     * <li>the first argument is positive infinity and the second argument is
     * less than zero,
     * </ul>
     * then the result is positive zero.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is positive zero and the second argument is less
     * than zero, or
     * <li>the first argument is positive infinity and the second argument is
     * greater than zero,
     * </ul>
     * then the result is positive infinity.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is
     * greater than zero but not a finite odd integer, or
     * <li>the first argument is negative infinity and the second argument is
     * less than zero but not a finite odd integer,
     * </ul>
     * then the result is positive zero.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is a
     * positive finite odd integer, or
     * <li>the first argument is negative infinity and the second argument is a
     * negative finite odd integer,
     * </ul>
     * then the result is negative zero.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is less
     * than zero but not a finite odd integer, or
     * <li>the first argument is negative infinity and the second argument is
     * greater than zero but not a finite odd integer,
     * </ul>
     * then the result is positive infinity.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is a
     * negative finite odd integer, or
     * <li>the first argument is negative infinity and the second argument is a
     * positive finite odd integer,
     * </ul>
     * then the result is negative infinity.
     * <p/>
     * <li>If the first argument is finite and less than zero
     * <ul>
     * <li>if the second argument is a finite even integer, the result is equal
     * to the result of raising the absolute value of the first argument to the
     * power of the second argument
     * <p/>
     * <li>if the second argument is a finite odd integer, the result is equal
     * to the negative of the result of raising the absolute value of the first
     * argument to the power of the second argument
     * <p/>
     * <li>if the second argument is finite and not an integer, then the result
     * is NaN.
     * </ul>
     * <p/>
     * <li>If both arguments are integers, then the result is exactly equal to
     * the mathematical result of raising the first argument to the power of the
     * second argument if that result can in fact be represented exactly as a
     * {@code double} value.
     * </ul>
     * <p/>
     * <p/>
     * (In the foregoing descriptions, a floating-point value is considered to
     * be an integer if and only if it is finite and a fixed point of the method
     * {@link #ceil ceil} or, equivalently, a fixed point of the method
     * {@link #floor floor}. A value is a fixed point of a one-argument method
     * if and only if the result of applying the method to the value is equal to
     * the value.)
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a the base.
     * @param b the exponent.
     * @return the value {@code a}<sup>{@code b}</sup>.
     */

    public static double pow(byte a, byte b) {
        return Math.pow(a, b);
    }

    public static double pow(short a, short b) {
        return Math.pow(a, b);
    }

    public static double pow(int a, int b) {
        return Math.pow(a, b);
    }

    public static double pow(long a, long b) {
        return Math.pow(a, b);
    }

    public static double pow(float a, float b) {
        return Math.pow(a, b);
    }

    public static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    // ---------------------------------------

    /**
     * Returns a {@code double} value with a positive sign, greater than or
     * equal to {@code 0.0} and less than {@code 1.0}. Returned values are
     * chosen pseudorandomly with (approximately) uniform distribution from that
     * range.
     * <p/>
     * <p/>
     * When this method is first called, it creates a single new
     * pseudorandom-number generator, exactly as if by the expression
     * <p/>
     * <blockquote>{@code new java.util.Random()}</blockquote>
     * <p/>
     * This new pseudorandom-number generator is used thereafter for all calls
     * to this method and is used nowhere else.
     * <p/>
     * <p/>
     * This method is properly synchronized to allow correct use by more than
     * one thread. However, if many threads need to generate pseudorandom
     * numbers at a great rate, it may reduce contention for each thread to have
     * its own pseudorandom-number generator.
     *
     * @return a pseudorandom {@code double} greater than or equal to
     *         {@code 0.0} and less than {@code 1.0}.
     */
    public static double random() {
        return Math.random();
    }

    /**
     * Returns the {@code double} value that is closest in value to the argument
     * and is equal to a mathematical integer. If two {@code double} values that
     * are mathematical integers are equally close, the result is the integer
     * value that is even. Special cases:
     * <ul>
     * <li>If the argument value is already equal to a mathematical integer,
     * then the result is the same as the argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative
     * zero, then the result is the same as the argument.
     * </ul>
     *
     * @param a a {@code double} value.
     * @return the closest floating-point value to {@code a} that is equal to a
     *         mathematical integer.
     */
    public static double rint(double a) {
        return Math.rint(a);
    }

    /**
     * Return {@code d} &times; 2<sup>{@code scaleFactor}</sup> rounded as if
     * performed by a single correctly rounded floating-point multiply to a
     * member of the double value set. See the Java Language Specification for a
     * discussion of floating-point value sets. If the exponent of the result is
     * between {@link Double#MIN_EXPONENT} and {@link Double#MAX_EXPONENT}, the
     * answer is calculated exactly. If the exponent of the result would be
     * larger than {@code Double.MAX_EXPONENT}, an infinity is returned. Note
     * that if the result is subnormal, precision may be lost; that is, when
     * {@code scalb(x, n)} is subnormal, {@code scalb(scalb(x, n), -n)} may not
     * equal <i>x</i>. When the result is non-NaN, the result has the same sign
     * as {@code d}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the first argument is NaN, NaN is returned.
     * <li>If the first argument is infinite, then an infinity of the same sign
     * is returned.
     * <li>If the first argument is zero, then a zero of the same sign is
     * returned.
     * </ul>
     *
     * @param d number to be scaled by a power of two.
     * @param scaleFactor power of 2 used to scale {@code d}
     * @return {@code d} &times; 2<sup>{@code scaleFactor}</sup>
     */
    public static double scalb(double d, int scaleFactor) {
        return Math.scalb(d, scaleFactor);
    }

    /**
     * Return {@code f} &times; 2<sup>{@code scaleFactor}</sup> rounded as if
     * performed by a single correctly rounded floating-point multiply to a
     * member of the float value set. See the Java Language Specification for a
     * discussion of floating-point value sets. If the exponent of the result is
     * between {@link Float#MIN_EXPONENT} and {@link Float#MAX_EXPONENT}, the
     * answer is calculated exactly. If the exponent of the result would be
     * larger than {@code Float.MAX_EXPONENT}, an infinity is returned. Note
     * that if the result is subnormal, precision may be lost; that is, when
     * {@code scalb(x, n)} is subnormal, {@code scalb(scalb(x, n), -n)} may not
     * equal <i>x</i>. When the result is non-NaN, the result has the same sign
     * as {@code f}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the first argument is NaN, NaN is returned.
     * <li>If the first argument is infinite, then an infinity of the same sign
     * is returned.
     * <li>If the first argument is zero, then a zero of the same sign is
     * returned.
     * </ul>
     *
     * @param f number to be scaled by a power of two.
     * @param scaleFactor power of 2 used to scale {@code f}
     * @return {@code f} &times; 2<sup>{@code scaleFactor}</sup>
     */
    public static float scalb(float f, int scaleFactor) {
        return Math.scalb(f, scaleFactor);
    }

    /**
     * Returns the signum function of the argument; zero if the argument is
     * zero, 1.0 if the argument is greater than zero, -1.0 if the argument is
     * less than zero.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive zero or negative zero, then the result is
     * the same as the argument.
     * </ul>
     *
     * @param d the floating-point value whose signum is to be returned
     * @return the signum function of the argument
     * @author Joseph D. Darcy
     */
    public static double signum(double d) {
        return Math.signum(d);
    }

    /**
     * Returns the signum function of the argument; zero if the argument is
     * zero, 1.0f if the argument is greater than zero, -1.0f if the argument is
     * less than zero.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive zero or negative zero, then the result is
     * the same as the argument.
     * </ul>
     *
     * @param f the floating-point value whose signum is to be returned
     * @return the signum function of the argument
     * @author Joseph D. Darcy
     */
    public static double signum(float f) {
        return Math.signum(f);
    }

    /**
     * Returns the trigonometric sine of an angle. Special cases:
     * <ul>
     * <li>If the argument is NaN or an infinity, then the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a an angle, in radians.
     * @return the sine of the argument.
     */
    public static double sin(double a) {
        return Math.sin(a);
    }

    /**
     * Returns the hyperbolic sine of a {@code double} value. The hyperbolic
     * sine of <i>x</i> is defined to be
     * (<i>e<sup>x</sup>&nbsp;-&nbsp;e<sup>-x</sup></i>)/2 where <i>e</i> is
     * {@linkplain Math#E Euler's number}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <p/>
     * <li>If the argument is NaN, then the result is NaN.
     * <p/>
     * <li>If the argument is infinite, then the result is an infinity with the
     * same sign as the argument.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 2.5 ulps of the exact result.
     *
     * @param x The number whose hyperbolic sine is to be returned.
     * @return The hyperbolic sine of {@code x}.
     */
    public static double sinh(double x) {
        return Math.sinh(x);
    }

    /**
     * Returns the correctly rounded positive square root of a {@code double}
     * value. Special cases:
     * <ul>
     * <li>If the argument is NaN or less than zero, then the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive
     * infinity.
     * <li>If the argument is positive zero or negative zero, then the result is
     * the same as the argument.
     * </ul>
     * Otherwise, the result is the {@code double} value closest to the true
     * mathematical square root of the argument value.
     *
     * @param a a value.
     * @return the positive square root of {@code a}. If the argument is NaN or
     *         less than zero, the result is NaN.
     */
    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    /**
     * Returns the trigonometric tangent of an angle. Special cases:
     * <ul>
     * <li>If the argument is NaN or an infinity, then the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results
     * must be semi-monotonic.
     *
     * @param a an angle, in radians.
     * @return the tangent of the argument.
     */
    public static double tan(double a) {
        return Math.tan(a);
    }

    /**
     * Returns the hyperbolic tangent of a {@code double} value. The hyperbolic
     * tangent of <i>x</i> is defined to be
     * (<i>e<sup>x</sup>&nbsp;-&nbsp;e<sup>-
     * x</sup></i>)/(<i>e<sup>x</sup>&nbsp;+&nbsp;e<sup>-x</sup></i>), in other
     * words, {@linkplain Math#sinh sinh(<i>x</i>)}/{@linkplain Math#cosh
     * cosh(<i>x</i>)}. Note that the absolute value of the exact tanh is always
     * less than 1.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <p/>
     * <li>If the argument is NaN, then the result is NaN.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign
     * as the argument.
     * <p/>
     * <li>If the argument is positive infinity, then the result is {@code +1.0}.
     * <p/>
     * <li>If the argument is negative infinity, then the result is {@code -1.0}.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 2.5 ulps of the exact result. The
     * result of {@code tanh} for any finite input must have an absolute value
     * less than or equal to 1. Note that once the exact result of tanh is
     * within 1/2 of an ulp of the limit value of &plusmn;1, correctly signed
     * &plusmn;{@code 1.0} should be returned.
     *
     * @param x The number whose hyperbolic tangent is to be returned.
     * @return The hyperbolic tangent of {@code x}.
     */
    public static double tanh(double x) {
        return Math.tanh(x);
    }

    /**
     * Converts an angle measured in radians to an approximately equivalent
     * angle measured in degrees. The conversion from radians to degrees is
     * generally inexact; users should <i>not</i> expect
     * {@code cos(toRadians(90.0))} to exactly equal {@code 0.0}.
     *
     * @param angrad an angle, in radians
     * @return the measurement of the angle {@code angrad} in degrees.
     */
    public static double toDegrees(double angrad) {
        return Math.toDegrees(angrad);
    }

    /**
     * Converts an angle measured in degrees to an approximately equivalent
     * angle measured in radians. The conversion from degrees to radians is
     * generally inexact.
     *
     * @param angdeg an angle, in degrees
     * @return the measurement of the angle {@code angdeg} in radians.
     */
    public static double toRadians(double angdeg) {
        return Math.toRadians(angdeg);
    }

    /**
     * Returns the size of an ulp of the argument. An ulp of a {@code double}
     * value is the positive distance between this floating-point value and the
     * {@code double} value next larger in magnitude. Note that for non-NaN
     * <i>x</i>, <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive or negative infinity, then the result is
     * positive infinity.
     * <li>If the argument is positive or negative zero, then the result is
     * {@code Double.MIN_VALUE}.
     * <li>If the argument is &plusmn;{@code Double.MAX_VALUE}, then the result
     * is equal to 2<sup>971</sup>.
     * </ul>
     *
     * @param d the floating-point value whose ulp is to be returned
     * @return the size of an ulp of the argument
     */
    public static double ulp(double d) {
        return Math.ulp(d);
    }

    /**
     * Returns the size of an ulp of the argument. An ulp of a {@code float}
     * value is the positive distance between this floating-point value and the
     * {@code float} value next larger in magnitude. Note that for non-NaN
     * <i>x</i>, <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive or negative infinity, then the result is
     * positive infinity.
     * <li>If the argument is positive or negative zero, then the result is
     * {@code Float.MIN_VALUE}.
     * <li>If the argument is &plusmn;{@code Float.MAX_VALUE}, then the result
     * is equal to 2<sup>104</sup>.
     * </ul>
     *
     * @param f the floating-point value whose ulp is to be returned
     * @return the size of an ulp of the argument
     */
    public static float ulp(float f) {
        return Math.ulp(f);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, true)          = [true]
     * ArrayUtils.add([true], false)       = [true, false]
     * ArrayUtils.add([true, false], true) = [true, false, true]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static boolean[] add(boolean[] array, boolean element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0, true)          = [true]
     * ArrayUtils.add([true], 0, false)       = [false, true]
     * ArrayUtils.add([false], 1, true)       = [false, true]
     * ArrayUtils.add([true, false], 1, true) = [true, true, false]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static boolean[] add(boolean[] array, int index, boolean element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static byte[] add(byte[] array, byte element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static byte[] add(byte[] array, int index, byte element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, '0')       = ['0']
     * ArrayUtils.add(['1'], '0')      = ['1', '0']
     * ArrayUtils.add(['1', '0'], '1') = ['1', '0', '1']
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static char[] add(char[] array, char element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0, 'a')            = ['a']
     * ArrayUtils.add(['a'], 0, 'b')           = ['b', 'a']
     * ArrayUtils.add(['a', 'b'], 0, 'c')      = ['c', 'a', 'b']
     * ArrayUtils.add(['a', 'b'], 1, 'k')      = ['a', 'k', 'b']
     * ArrayUtils.add(['a', 'b', 'c'], 1, 't') = ['a', 't', 'b', 'c']
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static char[] add(char[] array, int index, char element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static double[] add(double[] array, double element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add([1.1], 0, 2.2)              = [2.2, 1.1]
     * ArrayUtils.add([2.3, 6.4], 2, 10.5)        = [2.3, 6.4, 10.5]
     * ArrayUtils.add([2.6, 6.7], 0, -4.8)        = [-4.8, 2.6, 6.7]
     * ArrayUtils.add([2.9, 6.0, 0.3], 2, 1.0)    = [2.9, 6.0, 1.0, 0.3]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static double[] add(double[] array, int index, double element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static float[] add(float[] array, float element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static float[] add(float[] array, int index, float element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static int[] add(int[] array, int element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add([1], 0, 2)         = [2, 1]
     * ArrayUtils.add([2, 6], 2, 10)     = [2, 6, 10]
     * ArrayUtils.add([2, 6], 0, -4)     = [-4, 2, 6]
     * ArrayUtils.add([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static int[] add(int[] array, int index, int element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add([1L], 0, 2L)           = [2L, 1L]
     * ArrayUtils.add([2L, 6L], 2, 10L)      = [2L, 6L, 10L]
     * ArrayUtils.add([2L, 6L], 0, -4L)      = [-4L, 2L, 6L]
     * ArrayUtils.add([2L, 6L, 3L], 2, 1L)   = [2L, 6L, 1L, 3L]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static long[] add(long[] array, int index, long element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static long[] add(long[] array, long element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0, null)      = [null]
     * ArrayUtils.add(null, 0, "a")       = ["a"]
     * ArrayUtils.add(["a"], 1, null)     = ["a", null]
     * ArrayUtils.add(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static Object[] add(Object[] array, int index, Object element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element, unless the
     * element itself is null, in which case the return type is Object[]
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, null)      = [null]
     * ArrayUtils.add(null, "a")       = ["a"]
     * ArrayUtils.add(["a"], null)     = ["a", null]
     * ArrayUtils.add(["a"], "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array the array to "add" the element to, may be <code>null</code>
     * @param element the object to add, may be <code>null</code>
     * @return A new array containing the existing elements plus the new element
     *         The returned array type will be that of the input array (unless
     *         null), in which case it will have the same type as the element.
     */
    public static Object[] add(Object[] array, Object element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add([1], 0, 2)         = [2, 1]
     * ArrayUtils.add([2, 6], 2, 10)     = [2, 6, 10]
     * ArrayUtils.add([2, 6], 0, -4)     = [-4, 2, 6]
     * ArrayUtils.add([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static short[] add(short[] array, int index, short element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static short[] add(short[] array, short element) {
        return ArrayUtils.add(array, element);
    }

    public static Byte[] add(Byte[] array, int index, Byte element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Byte[] add(Byte[] array, Byte element) {
        return ArrayUtils.add(array, element);
    }

    public static Short[] add(Short[] array, Short element) {
        return ArrayUtils.add(array, element);
    }

    public static Short[] add(Short[] array, int index, Short element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Integer[] add(Integer[] array, int index, Integer element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Integer[] add(Integer[] array, Integer element) {
        return ArrayUtils.add(array, element);
    }

    public static Long[] add(Long[] array, Long element) {
        return ArrayUtils.add(array, element);
    }

    public static Long[] add(Long[] array, int index, Long element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Float[] add(Float[] array, int index, Float element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Float[] add(Float[] array, Float element) {
        return ArrayUtils.add(array, element);
    }

    public static Double[] add(Double[] array, int index, Double element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Double[] add(Double[] array, Double element) {
        return ArrayUtils.add(array, element);
    }

    public static Character[] add(Character[] array, int index, Character element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Character[] add(Character[] array, Character element) {
        return ArrayUtils.add(array, element);
    }

    public static Boolean[] add(Boolean[] array, int index, Boolean element) {
        return ArrayUtils.add(array, index, element);
    }

    public static Boolean[] add(Boolean[] array, Boolean element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0, null)      = [null]
     * ArrayUtils.add(null, 0, "a")       = ["a"]
     * ArrayUtils.add(["a"], 1, null)     = ["a", null]
     * ArrayUtils.add(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
     */
    public static Object[] addIgnoreNull(Object[] array, int index, Object element) {
        if (element != null) {
            return ArrayUtils.add(array, index, element);
        }
        return array;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.add(null, 0, null)      = [null]
     * ArrayUtils.add(null, 0, "a")       = ["a"]
     * ArrayUtils.add(["a"], 1, null)     = ["a", null]
     * ArrayUtils.add(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index > array.length).
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
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new boolean[] array.
     */
    public static boolean[] addAll(boolean[] array1, boolean[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new byte[] array.
     */
    public static byte[] addAll(byte[] array1, byte[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new char[] array.
     */
    public static char[] addAll(char[] array1, char[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new double[] array.
     */
    public static double[] addAll(double[] array1, double[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new float[] array.
     */
    public static float[] addAll(float[] array1, float[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new int[] array.
     */
    public static int[] addAll(int[] array1, int[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new long[] array.
     */
    public static long[] addAll(long[] array1, long[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
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
     * @param array1 the first array whose elements are added to the new array,
     *            may be <code>null</code>
     * @param array2 the second array whose elements are added to the new array,
     *            may be <code>null</code>
     * @return The new array, <code>null</code> if both arrays are
     *         <code>null</code>. The type of the new array is the type of the
     *         first array, unless the first array is null, in which case the
     *         type is the same as the second array.
     * @throws IllegalArgumentException if the array types are incompatible
     */
    public static Object[] addAll(Object[] array1, Object[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it
     * is always a new array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new short[] array.
     */
    public static short[] addAll(short[] array1, short[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    public static Byte[] addAll(Byte[] array1, Byte[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    public static Short[] addAll(Short[] array1, Short[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    public static Integer[] addAll(Integer[] array1, Integer[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    public static Long[] addAll(Long[] array1, Long[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    public static Float[] addAll(Float[] array1, Float[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    public static Double[] addAll(Double[] array1, Double[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    public static Character[] addAll(Character[] array1, Character[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove([true], 0)              = []
     * ArrayUtils.remove([true, false], 0)       = [false]
     * ArrayUtils.remove([true, false], 1)       = [true]
     * ArrayUtils.remove([true, true, false], 1) = [true, false]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static boolean[] remove(boolean[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove([true], 0)              = []
     * ArrayUtils.remove([true, false], 0)       = [false]
     * ArrayUtils.remove([true, false], 1)       = [true]
     * ArrayUtils.remove([true, true, false], 1) = [true, false]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static byte[] remove(byte[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove(['a'], 0)           = []
     * ArrayUtils.remove(['a', 'b'], 0)      = ['b']
     * ArrayUtils.remove(['a', 'b'], 1)      = ['a']
     * ArrayUtils.remove(['a', 'b', 'c'], 1) = ['a', 'c']
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static char[] remove(char[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static double[] remove(double[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static float[] remove(float[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static int[] remove(int[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static long[] remove(long[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove(["a"], 0)           = []
     * ArrayUtils.remove(["a", "b"], 0)      = ["b"]
     * ArrayUtils.remove(["a", "b"], 1)      = ["a"]
     * ArrayUtils.remove(["a", "b", "c"], 1) = ["a", "c"]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static Object[] remove(Object[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array the array to remove the element from, may not be
     *            <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0
     *             || index >= array.length), or if the array is
     *             <code>null</code>.
     */
    public static short[] remove(short[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Byte[] remove(Byte[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Short[] remove(Short[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Integer[] remove(Integer[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Long[] remove(Long[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Float[] remove(Float[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Double[] remove(Double[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Character[] remove(Character[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    public static Boolean[] remove(Boolean[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, true)                = null
     * ArrayUtils.removeElement([], true)                  = []
     * ArrayUtils.removeElement([true], false)             = [true]
     * ArrayUtils.removeElement([true, false], false)      = [true]
     * ArrayUtils.removeElement([true, false, true], true) = [false, true]
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static boolean[] removeElement(boolean[] array, boolean element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, 1)        = null
     * ArrayUtils.removeElement([], 1)          = []
     * ArrayUtils.removeElement([1], 0)         = [1]
     * ArrayUtils.removeElement([1, 0], 0)      = [1]
     * ArrayUtils.removeElement([1, 0, 1], 1)   = [0, 1]
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static byte[] removeElement(byte[] array, byte element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, 'a')            = null
     * ArrayUtils.removeElement([], 'a')              = []
     * ArrayUtils.removeElement(['a'], 'b')           = ['a']
     * ArrayUtils.removeElement(['a', 'b'], 'a')      = ['b']
     * ArrayUtils.removeElement(['a', 'b', 'a'], 'a') = ['b', 'a']
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static char[] removeElement(char[] array, char element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static double[] removeElement(double[] array, double element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static float[] removeElement(float[] array, float element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static int[] removeElement(int[] array, int element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static long[] removeElement(long[] array, long element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * 
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array the array to remove the element from, may be
     *            <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static short[] removeElement(short[] array, short element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array. <br />
     * <br />
     * <p/>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array. <br />
     * <br />
     * <p/>
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

    public static Byte[] removeElement(Byte[] array, Byte element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static Short[] removeElement(Short[] array, Short element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static Integer[] removeElement(Integer[] array, Integer element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static Long[] removeElement(Long[] array, Long element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static Float[] removeElement(Float[] array, Float element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static Double[] removeElement(Double[] array, Double element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static Character[] removeElement(Character[] array, Character element) {
        return ArrayUtils.removeElement(array, element);
    }

    public static Boolean[] removeElement(Boolean[] array, Boolean element) {
        return ArrayUtils.removeElement(array, element);
    }

    // <<< isEmpty section for arrays and Strings >>>

    /**
     * Checks if an array of Objects is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(Object[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of primitive bytes is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(byte[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of primitive chars is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(char[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of primitive shorts is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(short[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of primitive ints is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(int[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of primitive longs is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(long[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of primitive floats is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(float[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of primitive doubles is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(double[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of Dates is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(Date[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of BigDecimals is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(BigDecimal[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if an array of BigIntegers is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static boolean isEmpty(BigInteger[] array) {
        return ArrayUtils.isEmpty(array);
    }

    public static boolean isEmpty(Byte[] array) {
        return ArrayUtils.isEmpty(array);
    }

    public static boolean isEmpty(Short[] array) {
        return ArrayUtils.isEmpty(array);
    }
    
    public static boolean isEmpty(Integer[] array) {
        return ArrayUtils.isEmpty(array);
    }

    public static boolean isEmpty(Long[] array) {
        return ArrayUtils.isEmpty(array);
    }

    public static boolean isEmpty(Double[] array) {
        return ArrayUtils.isEmpty(array);
    }

    public static boolean isEmpty(Float[] array) {
        return ArrayUtils.isEmpty(array);
    }

    public static boolean isEmpty(Character[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Checks if a String is empty ("") or null.<br />
     * <br />
     * <code>
     * StringUtils.isEmpty(null)      = true <br />
     * StringUtils.isEmpty("")        = true <br />
     * StringUtils.isEmpty(" ")       = false <br />
     * StringUtils.isEmpty("bob")     = false <br />
     * StringUtils.isEmpty("  bob  ") = false <br />
     * </code>
     *
     * @param str the String to check, may be null
     * @return true if the String is empty or null
     */
    public static boolean isEmpty(String str) {
        return StringUtils.isBlank(str);
    }

    // <<< startsWith and endsWith for Strings >>>

    /**
     * Check if a String starts with a specified prefix.<br />
     * <br />
     * Two null references are considered to be equal. The comparison is case
     * sensitive.<br />
     * <br />
     * <code>
     * StringUtils.startsWith(null, null)      = true <br />
     * StringUtils.startsWith(null, "abc")     = false <br />
     * StringUtils.startsWith("abcdef", null)  = false <br />
     * StringUtils.startsWith("abcdef", "abc") = true <br />
     * StringUtils.startsWith("ABCDEF", "abc") = false <br />
     * </code>
     *
     * @param str the String to check, may be null
     * @param prefix the prefix to find, may be null
     * @return true if the String starts with the prefix, case sensitive, or
     *         both null
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
     * @param beginIndex the position to start from, negative means count back
     *            from the end of the String by this many characters
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
     * @param endIndex the position to end at (exclusive), negative means count
     *            back from the end of the String by this many characters
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
     * <p/>
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
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the substring with the string removed if found, null if null
     *         String input
     */
    public static String removeStart(String str, String remove) {
        return org.apache.commons.lang3.StringUtils.removeStart(str, remove);
    }

    /**
     * Removes a substring only if it is at the end of a source string,
     * otherwise returns the source string. <br />
     * <br />
     * <p/>
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
     *
     * @param str the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the String to search for and remove, may be null
     */
    public static String removeEnd(String str, String remove) {
        return StringUtils.removeEnd(str, remove);
    }

    // <<< lowerCase and upperCase functions >>>

    /**
     * Converts a String to lower case <br />
     * <br />
     * A null input String returns null. <br />
     * <br />
     * <p/>
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
     * Converts a String to upper case <br />
     * <br />
     * A null input String returns null.<br />
     * <br />
     * <p/>
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
     * <p/>
     * A null reference passed to this method is a no-op. <br />
     * <br />
     * <p/>
     * <code>
     * StringUtils.replace(null, *, *)        = null <br />
     * StringUtils.replace("", *, *)          = "" <br />
     * StringUtils.replace("any", null, *)    = "any" <br />
     * StringUtils.replace("any", *, null)    = "any" <br />
     * StringUtils.replace("any", "", *)      = "any" <br />
     * StringUtils.replace("aba", "a", null)  = "aba" <br />
     * StringUtils.replace("aba", "a", "")    = "b" <br />
     * StringUtils.replace("aba", "a", "z")   = "zbz" <br />
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
     * first max values of the search String.<br>
     * <br />
     * A null reference passed to this method is a no-op. <br />
     * <br />
     * <p/>
     * <code>
     * StringUtils.replace(null, *, *, *)         = null <br />
     * StringUtils.replace("", *, *, *)           = "" <br />
     * StringUtils.replace("any", null, *, *)     = "any" <br />
     * StringUtils.replace("any", *, null, *)     = "any"    <br />
     * StringUtils.replace("any", "", *, *)       = "any"    <br />
     * StringUtils.replace("any", *, *, 0)        = "any"    <br />
     * StringUtils.replace("abaa", "a", null, -1) = "abaa" <br />
     * StringUtils.replace("abaa", "a", "", -1)   = "b" <br />
     * StringUtils.replace("abaa", "a", "z", 0)   = "abaa" <br />
     * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa" <br />
     * StringUtils.replace("abaa", "a", "z", 2)   = "zbza" <br />
     * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz" <br />
     * </code>
     *
     * @param str text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement the String to replace it with, may be null
     * @param max maximum number of values to replace, or -1 if no maximum
     * @return the text with any replacements processed, null if null String
     *         input
     */
    public static String replace(String str, String searchString, String replacement, int max) {
        return StringUtils.replace(str, searchString, replacement, max);
    }

    public static Object[] flatten(Object... data) {
        List<Object> values = new ArrayList<Object>();
        Class<?> type = Void.class;
        for (Object obj : data) {
            if (obj == null) {
                values.add(null);
            } else if (obj.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(obj); i++) {
                    Object o = Array.get(obj, i);
                    Object[] flatten = flatten(o);
                    values.addAll(Arrays.asList(flatten));
                    type = getCommonSuperClass(type, flatten.getClass().getComponentType());
                }
            } else {
                values.add(obj);
                type = getCommonSuperClass(type, obj.getClass());
            }
        }

        Object[] result = (Object[]) Array.newInstance(type, 0);
        result = values.toArray(result);
        return result;
    }

    private static Class<?> getCommonSuperClass(Class<?> classA, Class<?> classB) {
        if (classA == Void.class) {
            return classB;
        } else if (classB == Void.class) {
            return classA;
        } else if (classA.isAssignableFrom(classB)) { // The most expected
                                                      // branch
            return classA;
        } else {
            Class<?> commonClass = classB;
            while (!commonClass.isAssignableFrom(classA)) {
                commonClass = commonClass.getSuperclass();
            }
            return commonClass;
        }
    }

    public static Object[] getValues(DomainOpenClass clazz) {
        IDomain<?> domain = clazz.getDomain();
        List<Object> values = new ArrayList<Object>();
        for (Object item : domain) {
            values.add(item);
        }

        Class<?> type = clazz.getInstanceClass();
        Object[] result = (Object[]) Array.newInstance(type, 0);
        result = values.toArray(result);
        return result;
    }
}
