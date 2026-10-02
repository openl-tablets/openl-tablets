/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodDetailsMethodCaller;
import org.openl.binding.impl.cast.MethodSearchTuner;
import org.openl.binding.impl.cast.VOID;
import org.openl.domain.IDomain;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenLUserRuntimeException;
import org.openl.rules.annotations.ArrayResultType;
import org.openl.rules.annotations.IgnoreNonVarargsMatching;
import org.openl.rules.cloner.Cloner;
import org.openl.types.impl.StaticDomainOpenClass;
import org.openl.util.ArrayTool;
import org.openl.util.DateTool;
import org.openl.util.math.MathUtils;

/**
 * This class is connected to rules and all these methods can be used from rules.
 *
 * @author snshor
 */
public final class RulesUtils {

    private RulesUtils() {
    }

    // SMALL

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Byte small(Byte[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Short small(Short[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Integer small(Integer[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Long small(Long[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Float small(Float[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Double small(Double[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static BigInteger small(BigInteger[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static BigDecimal small(BigDecimal[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Byte small(byte[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Short small(short[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Integer small(int[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Long small(long[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Float small(float[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Double small(double[] values, int position) {
        return MathUtils.small(values, position);
    }

    // BIG

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Byte big(Byte[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Short big(Short[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Integer big(Integer[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Long big(Long[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Float big(Float[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Double big(Double[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static BigInteger big(BigInteger[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static BigDecimal big(BigDecimal[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Byte big(byte[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Short big(short[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Integer big(int[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Long big(long[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Float big(float[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>. A NaN value makes the result NaN.
     * </p>
     *
     * @param values   an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Double big(double[] values, int position) {
        return MathUtils.big(values, position);
    }

    // MEDIAN

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(Byte[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(Short[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(Integer[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(Long[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped. A NaN value makes the result NaN.
     */
    public static Float median(Float[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped. A NaN value makes the result NaN.
     */
    public static Double median(Double[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static BigDecimal median(BigInteger[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static BigDecimal median(BigDecimal[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(byte[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(short[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(int[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped.
     */
    public static Double median(long[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped. A NaN value makes the result NaN.
     */
    public static Float median(float[] values) {
        return MathUtils.median(values);
    }

    /**
     * Returns the median of the values: the middle one, or the average of the two middle ones. Empty values are
     * skipped. A NaN value makes the result NaN.
     */
    public static Double median(double[] values) {
        return MathUtils.median(values);
    }

    // <<< Contains Functions >>>

    /**
     * <p>
     * Checks if the object is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param obj   the object to find
     * @return <code>true</code> if the array contains the object
     */
    public static <T> boolean contains(T[] array, T obj) {
        return ArrayUtils.contains(array, obj);
    }

    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p/>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem  the value to find
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem  the value to find
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem  the value to find
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem  the value to find
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem  the value to find
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem  the value to find
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
     * </p>
     *
     * @param array the array to search through
     * @param elem  the value to find
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

    public static boolean contains(Date[] array, Date elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(String[] array, String elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(Character[] array, Character elem) {
        return ArrayUtils.contains(array, elem);
    }

    public static boolean contains(IntRange[] array, Integer elem) {
        return containsInRanges(array, range -> range.contains(elem));
    }

    public static boolean contains(IntRange[] array, Long elem) {
        return containsInRanges(array, range -> range.contains(elem));
    }

    public static boolean contains(IntRange[] array, BigInteger elem) {
        return containsInRanges(array, range -> range.contains(elem));
    }

    private static boolean containsInRanges(IntRange @Nullable [] array, Predicate<IntRange> rangeContains) {
        return array != null && Arrays.stream(array).anyMatch(range -> range != null && rangeContains.test(range));
    }

    public static boolean contains(DoubleRange[] array, Double elem) {
        if (array == null) {
            return false;
        }
        for (DoubleRange range : array) {
            if (range != null && range.contains(elem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(CharRange[] array, Character elem) {
        if (array == null) {
            return false;
        }
        for (CharRange range : array) {
            if (range != null && range.contains(elem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(DateRange[] array, Date elem) {
        if (array == null) {
            return false;
        }
        for (DateRange range : array) {
            if (range != null && range.contains(elem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(StringRange[] array, CharSequence elem) {
        if (array == null) {
            return false;
        }
        for (StringRange range : array) {
            if (range != null && range.contains(elem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(StringRange[] array, String elem) {
        return contains(array, (CharSequence) elem);
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

    public static boolean contains(IntRange[] ary1, Integer[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (Integer elem : ary2) {
            if (elem != null && !contains(ary1, elem)) {
                return false;
            }
        }
        return Arrays.stream(ary2).anyMatch(Objects::nonNull);
    }

    public static boolean contains(IntRange[] ary1, int[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (Integer elem : ary2) {
            if (!contains(ary1, elem)) {
                return false;
            }
        }
        return ary2.length > 0;
    }

    public static boolean contains(DoubleRange[] ary1, Double[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (Double elem : ary2) {
            if (elem != null && !contains(ary1, elem)) {
                return false;
            }
        }
        return Arrays.stream(ary2).anyMatch(Objects::nonNull);
    }

    public static boolean contains(DoubleRange[] ary1, double[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (Double elem : ary2) {
            if (!contains(ary1, elem)) {
                return false;
            }
        }
        return ary2.length > 0;
    }

    public static boolean contains(CharRange[] ary1, Character[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (Character elem : ary2) {
            if (elem != null && !contains(ary1, elem)) {
                return false;
            }
        }
        return Arrays.stream(ary2).anyMatch(Objects::nonNull);
    }

    public static boolean contains(CharRange[] ary1, char[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (Character elem : ary2) {
            if (!contains(ary1, elem)) {
                return false;
            }
        }
        return ary2.length > 0;
    }

    public static boolean contains(StringRange[] ary1, CharSequence[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (CharSequence elem : ary2) {
            if (elem != null && !contains(ary1, elem)) {
                return false;
            }
        }
        return Arrays.stream(ary2).anyMatch(Objects::nonNull);
    }

    public static boolean contains(StringRange[] ary1, String[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (String elem : ary2) {
            if (elem != null && !contains(ary1, elem)) {
                return false;
            }
        }
        return Arrays.stream(ary2).anyMatch(Objects::nonNull);
    }

    public static boolean contains(DateRange[] ary1, Date[] ary2) {
        if (ary2 == null) {
            return false;
        }
        for (Date elem : ary2) {
            if (elem != null && !contains(ary1, elem)) {
                return false;
            }
        }
        return Arrays.stream(ary2).anyMatch(Objects::nonNull);
    }

    public static boolean contains(IntRange range, Long x) {
        return range != null && range.contains(x);
    }

    public static boolean contains(IntRange range, BigInteger x) {
        return range != null && range.contains(x);
    }

    public static boolean contains(DoubleRange range, Double x) {
        return range != null && range.contains(x);
    }

    public static boolean contains(DoubleRange range, BigDecimal x) {
        return range != null && range.contains(x);
    }

    public static boolean contains(CharRange range, Character x) {
        return range != null && range.contains(x);
    }

    public static boolean contains(StringRange range, String x) {
        return range != null && range.contains(x);
    }

    public static boolean contains(DateRange range, Date x) {
        return range != null && range.contains(x);
    }

    /**
     * <p>
     * Finds the index of the given object in the array.
     * </p>
     * <p/>
     * <p>
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param obj   the object to find, may be <code>null</code>
     * @return the index of the object within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param elem  the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param elem  the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param elem  the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param elem  the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param elem  the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param elem  the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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
     * This method returns {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) for a <code>null</code> input array.
     * </p>
     *
     * @param array the array to search through for the object, may be <code>null</code>
     * @param elem  the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     * found or <code>null</code> array input
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

    public static VOID error(String msg) {
        throw new OpenLUserRuntimeException(msg);
    }

    public static VOID error(String code, String message) {
        throw new OpenLUserRuntimeException(code, message);
    }

    public static VOID error(Object object) {
        throw new OpenLUserRuntimeException(object);
    }

    public static String[] intersection(String[] ary1, String[] ary2) {
        return ArrayTool.intersection(ary1, ary2);
    }

    public static void out(String output) {
        println(output);
    }

    public static void out(Object output) {
        println(output);
    }

    public static void out(byte output) {
        println(output);
    }

    public static void out(short output) {
        println(output);
    }

    public static void out(int output) {
        println(output);
    }

    public static void out(long output) {
        println(output);
    }

    public static void out(float output) {
        println(output);
    }

    public static void out(double output) {
        println(output);
    }

    public static void out(char output) {
        println(output);
    }

    public static void out(boolean output) {
        println(output);
    }

    // The out rule functions print to the standard output by their contract, which logging would break.
    @SuppressWarnings("java:S106")
    private static void println(Object output) {
        System.out.println(output);
    }

    public static Integer absMonth(Date d) {
        return DateTool.absMonth(d);
    }

    public static Integer absQuarter(Date d) {
        return DateTool.absQuarter(d);
    }

    public static Integer dayDiff(Date d1, Date d2) {
        return DateTool.dayDiff(d1, d2);
    }

    public static Integer dayOfMonth(Date d) {
        return DateTool.dayOfMonth(d);
    }

    public static Date firstDateOfQuarter(int absQuarter) {
        return DateTool.firstDateOfQuarter(absQuarter);
    }

    public static Date lastDateOfQuarter(int absQuarter) {
        return DateTool.lastDateOfQuarter(absQuarter);
    }

    public static Integer lastDayOfMonth(Date d) {
        return DateTool.lastDayOfMonth(d);
    }

    public static Integer month(Date d) {
        return DateTool.month(d);
    }

    public static Integer monthDiff(Date d1, Date d2) {
        return DateTool.monthDiff(d1, d2);
    }

    public static Integer yearDiff(Date d1, Date d2) {
        return DateTool.yearDiff(d1, d2);
    }

    public static Integer weekDiff(Date d1, Date d2) {
        return DateTool.weekDiff(d1, d2);
    }

    public static Integer quarter(Date d) {
        return DateTool.quarter(d);
    }

    public static Integer year(Date d) {
        return DateTool.year(d);
    }

    public static Integer dayOfWeek(Date d) {
        return DateTool.dayOfWeek(d);
    }

    public static Integer dayOfYear(Date d) {
        return DateTool.dayOfYear(d);
    }

    public static Integer weekOfYear(Date d) {
        return DateTool.weekOfYear(d);
    }

    public static Integer weekOfMonth(Date d) {
        return DateTool.weekOfMonth(d);
    }

    public static Integer second(Date d) {
        return DateTool.second(d);
    }

    public static Integer minute(Date d) {
        return DateTool.minute(d);
    }

    /**
     * @param d Date
     * @return hour from 0 to 12
     */
    public static Integer hour(Date d) {
        return DateTool.hour(d);
    }

    /**
     * @param d Date
     * @return hour from 0 to 24
     */
    public static Integer hourOfDay(Date d) {
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

    /**
     * Returns a copy of the array without the element at the given position. The elements after it move one position
     * to the left.
     * <p>
     * The copy has the type of the array, a primitive array such as {@code int[]} included. A missing array gives an
     * empty result.
     *
     * <pre>
     * remove(null, 0)            = null
     * remove(["a"], 0)           = []
     * remove(["a", "b"], 1)      = ["a"]
     * remove(["a", "b", "c"], 1) = ["a", "c"]
     * </pre>
     *
     * @param array the array to remove the element from, may be {@code null}
     * @param index the position of the element to remove, counted from 0
     * @return a new array without the element, or {@code null} when the array is {@code null}
     * @throws IndexOutOfBoundsException if the array is not {@code null} and the index is out of range
     *                                   ({@code index < 0 || index >= array.length})
     */
    // Rule function: a null array gives a null result, as for the other array functions.
    @SuppressWarnings("java:S1168")
    @ArrayResultType(ArrayResultType.Kind.SAME)
    public static <T> T @Nullable [] remove(T @Nullable [] array, int index) {
        return array == null ? null : ArrayUtils.remove(array, index);
    }

    // <<< isEmpty section for arrays and Strings >>>

    // <<< startsWith and endsWith for Strings >>>

    // <<< subString >>>

    // <<< removeStart and removeEnd >>>

    /*
     * into the return statement the full path of StringUtils should be written if we write StringUtils.removeStart(str,
     * remove); OpenL Studio won't work correctly.
     */

    // <<< lowerCase and upperCase functions >>>

    // <<< replace functions for Strings >>>

    /**
     * Returns the elements of the arrays and the values in one array, in their order. An array of arrays gives the
     * elements of every level, and a missing array gives no elements. When every argument is a missing array, the
     * result is missing too.
     * <p>
     * The result has the closest common type of the elements: {@code int[][]} gives {@code int[]}, and an
     * {@code int[]} with an {@code Integer[]} gives {@code Integer[]}.
     */
    @MethodSearchTuner(wrapper = FlattenMethodCallerWrapper.class, methodFilter = FlattenMethodFilter.class)
    @IgnoreNonVarargsMatching
    public static @Nullable Object flatten(Object... data) {
        var flattenMethodDetails = (FlattenMethodDetails) MethodDetailsMethodCaller.getMethodDetails();
        var dims = flattenMethodDetails.getDims();
        var values = new ArrayList<Object>();
        var missing = true;
        for (var i = 0; i < data.length; i++) {
            missing &= dims[i] > 0 && data[i] == null;
            var openCast = flattenMethodDetails.getOpenCasts()[i];
            values.addAll(flattenInternal(dims[i], data[i]).stream().map(openCast::convert).toList());
        }
        if (missing) {
            return null;
        }
        var result = Array.newInstance(flattenMethodDetails.getType().getComponentClass().getInstanceClass(),
                values.size());
        for (var i = 0; i < values.size(); i++) {
            Array.set(result, i, values.get(i));
        }
        return result;
    }

    private static List<Object> flattenInternal(int dim, Object v) {
        if (dim == 0) {
            return Collections.singletonList(v);
        } else {
            if (v == null || Array.getLength(v) == 0) {
                return Collections.emptyList();
            }
            var values = new ArrayList<Object>();
            for (var i = 0; i < Array.getLength(v); i++) {
                values.addAll(flattenInternal(dim - 1, Array.get(v, i)));
            }
            return values;
        }
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>arrays</code>. When an array is returned, it is always a new
     * array.
     * </p>
     * <p/>
     *
     * <pre>
     * RuleUtils.addAll(null, null)     = [null, null]
     * RuleUtils.addAll(array1, null)   = cloned copy of array1 with additional null element in the end of array
     * RuleUtils.addAll(null, array2)   = cloned copy of array2 with additional null element in the beginning of the array
     * RuleUtils.addAll([], [])         = []
     * RuleUtils.addAll([null], [null]) = [null, null]
     * RuleUtils.addAll(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
     * </pre>
     *
     * @param arrays the arrays whose elements are added to the new array, may be <code>null</code>
     * @return The new array, <code>null</code> when there are no arguments. Its element type is the closest common type
     * of the array elements and the added elements: an <code>int[]</code> with an <code>int</code> gives an
     * <code>int[]</code>, with an <code>Integer</code> an <code>Integer[]</code>, and with a <code>double</code> a
     * <code>double[]</code>.
     */
    @MethodSearchTuner(wrapper = AddAllMethodCallerWrapper.class, methodFilter = AddAllMethodFilter.class)
    @IgnoreNonVarargsMatching
    public static Object addAll(Object... arrays) {
        if (arrays == null || arrays.length == 0) {
            return null;
        }
        var addAllMethodDetails = (AddAllMethodDetails) MethodDetailsMethodCaller.getMethodDetails();
        var totalLength = getTotalLength(arrays, addAllMethodDetails);
        Object result = Array.newInstance(addAllMethodDetails.getType().getComponentClass().getInstanceClass(),
                totalLength);
        var p = 0;
        for (var i = 0; i < arrays.length; i++) {
            if (!addAllMethodDetails.getParamsAsElement()[i]) {
                if (arrays[i] != null) {
                    var length = Array.getLength(arrays[i]);
                    for (var j = 0; j < length; j++) {
                        var openCast = addAllMethodDetails.getOpenCasts()[i];
                        Object v = Array.get(arrays[i], j);
                        Array.set(result, p, castElement(openCast, v));
                        p++;
                    }
                }
            } else {
                var openCast = addAllMethodDetails.getOpenCasts()[i];
                Array.set(result, p, castElement(openCast, arrays[i]));
                p++;
            }
        }
        return result;
    }

    /**
     * Counts the elements of the joined array: all elements of an array argument and one for an argument added as an
     * element.
     */
    private static int getTotalLength(Object[] arrays, AddAllMethodDetails addAllMethodDetails) {
        var totalLength = 0;
        for (var i = 0; i < arrays.length; i++) {
            if (!addAllMethodDetails.getParamsAsElement()[i]) {
                if (arrays[i] != null) {
                    totalLength = totalLength + Array.getLength(arrays[i]);
                }
            } else {
                totalLength++;
            }
        }
        return totalLength;
    }

    private static Object castElement(IOpenCast openCast, Object value) {
        return openCast != null ? openCast.convert(value) : value;
    }

    @MethodSearchTuner(wrapper = AddAllMethodCallerWrapper.class, methodFilter = AddAllMethodFilter.class)
    @IgnoreNonVarargsMatching
    public static Object add(Object... arrays) {
        return addAll(arrays);
    }

    @MethodSearchTuner(wrapper = GetValuesMethodCallerWrapper.class)
    public static Object getValues(StaticDomainOpenClass staticDomainOpenClass) {
        IDomain<?> domain = staticDomainOpenClass.getDomain();
        var size = 0;
        for (Object item : domain) {
            size++;
        }

        Class<?> type = staticDomainOpenClass.getDelegate().getInstanceClass();
        Object result = Array.newInstance(type, size);
        var i = 0;
        for (Object item : domain) {
            Array.set(result, i, item);
            i++;
        }
        return result;
    }

    public static boolean instanceOf(Object o, Class<?> clazz) {
        if (o == null) {
            return false;
        }
        if (clazz == null) {
            return false;
        }
        return clazz.isAssignableFrom(o.getClass());
    }

    public static Object staticField(Object instance, String fieldName) {
        try {
            Class<?> aClass = instance != null ? instance.getClass() : null;
            while (aClass != null) {
                var declaredFields = aClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    if (field.getName().equals(fieldName) && Modifier
                            .isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                        return field.get(null);
                    }
                }
                aClass = aClass.getSuperclass();
            }
        } catch (IllegalAccessException e) {
            throw new OpenLRuntimeException("%s '%s'.".formatted(instance, fieldName));
        }
        return null;
    }

    public static <T> T copy(T origin) {
        return Cloner.clone(origin);
    }
}
