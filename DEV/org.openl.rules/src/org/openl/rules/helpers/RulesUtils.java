/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.helpers;

import java.io.*;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.openl.binding.impl.cast.AutoCastReturnType;
import org.openl.binding.impl.cast.DefaultAutoCastFactory.ReturnType;
import org.openl.binding.impl.cast.ThrowableVoidCast.ThrowableVoid;
import org.openl.domain.IDomain;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.rules.testmethod.OpenLUserRuntimeException;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.ArrayTool;
import org.openl.util.CollectionUtils;
import org.openl.util.DateTool;
import org.openl.util.IOUtils;
import org.openl.util.math.MathUtils;

/**
 * This class is connected to rules and all these methods can be used from rules.
 *
 * @author snshor
 */
public final class RulesUtils {

    private RulesUtils() {
    }

    public static final String DEFAULT_DOUBLE_FORMAT = "#,##0.00";

    public static final double E = Math.E;
    public static final double PI = Math.PI;

    // SMALL

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
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
     * @param values an array, must not be null or empty
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
     * @param values an array, must not be null or empty
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
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Long small(long[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Float small(float[] values, int position) {
        return MathUtils.small(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in ascending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
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
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static java.lang.Byte big(java.lang.Byte[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
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
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
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
     * @param values an array, must not be null or empty
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
     * @param values an array, must not be null or empty
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
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Long big(long[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Float big(float[] values, int position) {
        return MathUtils.big(values, position);
    }

    /**
     * <p>
     * Removes null values from array, sorts an array in descending order and returns the value at position
     * <i>'position'</i>
     * </p>
     *
     * @param values an array, must not be null or empty
     * @param position array index whose value we wand to get
     * @return value from array at position <i>'position'</i>
     */
    public static Double big(double[] values, int position) {
        return MathUtils.big(values, position);
    }

    // MEDIAN

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Double median(java.lang.Byte[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Double median(java.lang.Short[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Double median(java.lang.Integer[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static java.lang.Double median(java.lang.Long[] values) {
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
    public static java.math.BigDecimal median(java.math.BigInteger[] values) {
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
    public static Double median(byte[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static Double median(short[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static Double median(int[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static Double median(long[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static Float median(float[] values) {
        return MathUtils.median(values);
    }

    /**
     * "Method median is not implemented yet"
     *
     * @param values
     * @return
     */
    public static Double median(double[] values) {
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

    // SORT
    /**
     * Sorts the specified array into ascending order, according to the {@linkplain Comparable natural ordering} of its
     * elements. All elements in the array must implement the {@link Comparable} interface. Furthermore, all elements in
     * the array must be <i>mutually comparable</i> (that is, {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the array).
     * <p/>
     * <p/>
     * This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result of the sort.
     * <p/>
     * <p/>
     * Implementation note: This implementation is a stable, adaptive, iterative mergesort that requires far fewer than
     * n lg(n) comparisons when the input array is partially sorted, while offering the performance of a traditional
     * mergesort when the input array is randomly ordered. If the input array is nearly sorted, the implementation
     * requires approximately n comparisons. Temporary storage requirements vary from a small constant for nearly sorted
     * input arrays to n/2 object references for randomly ordered input arrays.
     * <p/>
     * <p/>
     * The implementation takes equal advantage of ascending and descending order in its input array, and can take
     * advantage of ascending and descending order in different parts of the the same input array. It is well-suited to
     * merging two or more sorted arrays: simply concatenate the arrays and sort the resulting array.
     * <p/>
     * <p/>
     * The implementation was adapted from Tim Peters's list sort for Python
     * (<a href= "http://svn.python.org/projects/python/trunk/Objects/listsort.txt"> TimSort</a>). It uses techiques
     * from Peter McIlroy's "Optimistic Sorting and Information Theoretic Complexity", in Proceedings of the Fourth
     * Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474, January 1993.
     *
     * @param values the array to be sorted
     * @return a sorted array
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<?>> T[] sort(T[] values) {
        T[] sortedArray = null;

        if (values != null) {
            sortedArray = (T[]) Array.newInstance(values.getClass().getComponentType(), values.length);
            T[] notNullArray = ArrayTool.removeNulls(values);
            if (notNullArray == values) {
                notNullArray = values.clone();
            }
            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;

    }

    /**
     * Sorts the specified array into ascending numerical order.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy, Jon Bentley, and
     * Joshua Bloch. This algorithm offers O(n log(n)) performance on many data sets that cause other quicksorts to
     * degrade to quadratic performance, and is typically faster than traditional (one-pivot) Quicksort implementations.
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
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy, Jon Bentley, and
     * Joshua Bloch. This algorithm offers O(n log(n)) performance on many data sets that cause other quicksorts to
     * degrade to quadratic performance, and is typically faster than traditional (one-pivot) Quicksort implementations.
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
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy, Jon Bentley, and
     * Joshua Bloch. This algorithm offers O(n log(n)) performance on many data sets that cause other quicksorts to
     * degrade to quadratic performance, and is typically faster than traditional (one-pivot) Quicksort implementations.
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
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy, Jon Bentley, and
     * Joshua Bloch. This algorithm offers O(n log(n)) performance on many data sets that cause other quicksorts to
     * degrade to quadratic performance, and is typically faster than traditional (one-pivot) Quicksort implementations.
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
     * The {@code <} relation does not provide a total order on all double values: {@code -0.0d == 0.0d} is {@code true}
     * and a {@code Double.NaN} value compares neither less than, greater than, nor equal to any value, even itself.
     * This method uses the total order imposed by the method {@link Double#compareTo}: {@code -0.0d} is treated as less
     * than value {@code 0.0d} and {@code Double.NaN} is considered greater than any other value and all
     * {@code Double.NaN} values are considered equal.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy, Jon Bentley, and
     * Joshua Bloch. This algorithm offers O(n log(n)) performance on many data sets that cause other quicksorts to
     * degrade to quadratic performance, and is typically faster than traditional (one-pivot) Quicksort implementations.
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
     * The {@code <} relation does not provide a total order on all double values: {@code -0.0d == 0.0d} is {@code true}
     * and a {@code Double.NaN} value compares neither less than, greater than, nor equal to any value, even itself.
     * This method uses the total order imposed by the method {@link Double#compareTo}: {@code -0.0d} is treated as less
     * than value {@code 0.0d} and {@code Double.NaN} is considered greater than any other value and all
     * {@code Double.NaN} values are considered equal.
     * <p/>
     * <p/>
     * Implementation note: The sorting algorithm is a Dual-Pivot Quicksort by Vladimir Yaroslavskiy, Jon Bentley, and
     * Joshua Bloch. This algorithm offers O(n log(n)) performance on many data sets that cause other quicksorts to
     * degrade to quadratic performance, and is typically faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param - values the array to be sorted
     * @return a sorted array of doubles
     */
    public static double[] sort(double[] values) {
        return MathUtils.sort(values);
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
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
     * The method returns <code>false</code> if a <code>null</code> array is passed in.
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
        if (array == null) {
            return false;
        }
        for (IntRange range : array) {
            if (range != null && range.contains(elem)) {
                return true;
            }
        }
        return false;
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
     * @param obj the object to find, may be <code>null</code>
     * @return the index of the object within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
     * @param elem the value to find
     * @return the index of the value within the array, {@link ArrayUtils#INDEX_NOT_FOUND} (<code>-1</code>) if not
     *         found or <code>null</code> array input
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
        return CollectionUtils.isNotEmpty(values) && !CollectionUtils.hasNull(values);
    }

    public static ThrowableVoid error(String msg) {
        throw new OpenLUserRuntimeException(msg);
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
    @Deprecated
    public static String dateToString(Date date, String dateFormat) {
        String stringDate = "Incorrect date format";
        try {
            stringDate = DateTool.dateToString(date, dateFormat);
        } catch (Exception e) {
            throw new OpenLRuntimeException(String.format("%s '%s'", stringDate, dateFormat));
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
    @Deprecated
    public static String dateToString(Date date) {
        String stringDate = "Incorrect date format";
        try {
            stringDate = DateTool.dateToString(date);
        } catch (Exception e) {
            throw new OpenLRuntimeException(stringDate);
        }
        return stringDate;
    }

    @Deprecated
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
     * Parse the represented string value to the double. Uses default Locale for it.
     * <p/>
     * Shouldn`t be used.
     */
    @Deprecated
    public static double parseFormattedDouble(String s) throws ParseException {
        return parseFormattedDouble(s, DEFAULT_DOUBLE_FORMAT);
    }

    /**
     * Parse the represented string value to the double. Uses default Locale for it. See
     * {@link DecimalFormat#DecimalFormat(String)}
     * <p/>
     * Shouldn`t be used.
     */
    @Deprecated
    public static double parseFormattedDouble(String s, String fmt) throws ParseException {
        DecimalFormat df = new DecimalFormat(fmt);
        return df.parse(s).doubleValue();
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

    // Math functions

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
     * Returns the absolute value of a {@code double} value. If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned. Special cases:
     * <ul>
     * <li>If the argument is positive zero or negative zero, the result is positive zero.
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
     * Returns the absolute value of a {@code float} value. If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned. Special cases:
     * <ul>
     * <li>If the argument is positive zero or negative zero, the result is positive zero.
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
     * Returns the absolute value of an {@code int} value. If the argument is not negative, the argument is returned. If
     * the argument is negative, the negation of the argument is returned.
     * <p/>
     * <p/>
     * Note that if the argument is equal to the value of {@link Integer#MIN_VALUE}, the most negative representable
     * {@code int} value, the result is that same value, which is negative.
     *
     * @param a the argument whose absolute value is to be determined
     * @return the absolute value of the argument.
     */
    public static int abs(int a) {
        return Math.abs(a);
    }

    /**
     * Returns the absolute value of a {@code long} value. If the argument is not negative, the argument is returned. If
     * the argument is negative, the negation of the argument is returned.
     * <p/>
     * <p/>
     * Note that if the argument is equal to the value of {@link Long#MIN_VALUE} , the most negative representable
     * {@code long} value, the result is that same value, which is negative.
     *
     * @param a the argument whose absolute value is to be determined
     * @return the absolute value of the argument.
     */
    public static long abs(long a) {
        return Math.abs(a);
    }

    /**
     * Returns the arc cosine of a value; the returned angle is in the range 0.0 through <i>pi</i>. Special case:
     * <ul>
     * <li>If the argument is NaN or its absolute value is greater than 1, then the result is NaN.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a the value whose arc cosine is to be returned.
     * @return the arc cosine of the argument.
     */
    public static double acos(double a) {
        return Math.acos(a);
    }

    /**
     * Returns the arc sine of a value; the returned angle is in the range -<i>pi</i>/2 through <i>pi</i>/2. Special
     * cases:
     * <ul>
     * <li>If the argument is NaN or its absolute value is greater than 1, then the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a the value whose arc sine is to be returned.
     * @return the arc sine of the argument.
     */
    public static double asin(double a) {
        return Math.asin(a);
    }

    /**
     * Returns the arc tangent of a value; the returned angle is in the range -<i>pi</i>/2 through <i>pi</i>/2. Special
     * cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a the value whose arc tangent is to be returned.
     * @return the arc tangent of the argument.
     */
    public static double atan(double a) {
        return Math.atan(a);
    }

    /**
     * Returns the angle <i>theta</i> from the conversion of rectangular coordinates ({@code x},&nbsp;{@code y}) to
     * polar coordinates (r,&nbsp;<i>theta</i>). This method computes the phase <i>theta</i> by computing an arc tangent
     * of {@code y/x} in the range of -<i>pi</i> to <i>pi</i>. Special cases:
     * <ul>
     * <li>If either argument is NaN, then the result is NaN.
     * <li>If the first argument is positive zero and the second argument is positive, or the first argument is positive
     * and finite and the second argument is positive infinity, then the result is positive zero.
     * <li>If the first argument is negative zero and the second argument is positive, or the first argument is negative
     * and finite and the second argument is positive infinity, then the result is negative zero.
     * <li>If the first argument is positive zero and the second argument is negative, or the first argument is positive
     * and finite and the second argument is negative infinity, then the result is the {@code double} value closest to
     * <i>pi</i>.
     * <li>If the first argument is negative zero and the second argument is negative, or the first argument is negative
     * and finite and the second argument is negative infinity, then the result is the {@code double} value closest to
     * -<i>pi</i>.
     * <li>If the first argument is positive and the second argument is positive zero or negative zero, or the first
     * argument is positive infinity and the second argument is finite, then the result is the {@code double} value
     * closest to <i>pi</i>/2.
     * <li>If the first argument is negative and the second argument is positive zero or negative zero, or the first
     * argument is negative infinity and the second argument is finite, then the result is the {@code double} value
     * closest to -<i>pi</i>/2.
     * <li>If both arguments are positive infinity, then the result is the {@code double} value closest to <i>pi</i>/4.
     * <li>If the first argument is positive infinity and the second argument is negative infinity, then the result is
     * the {@code double} value closest to 3*<i>pi</i>/4.
     * <li>If the first argument is negative infinity and the second argument is positive infinity, then the result is
     * the {@code double} value closest to -<i>pi</i>/4.
     * <li>If both arguments are negative infinity, then the result is the {@code double} value closest to
     * -3*<i>pi</i>/4.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 2 ulps of the exact result. Results must be semi-monotonic.
     *
     * @param y the ordinate coordinate
     * @param x the abscissa coordinate
     * @return the <i>theta</i> component of the point (<i>r</i>,&nbsp;<i>theta</i>) in polar coordinates that
     *         corresponds to the point (<i>x</i>,&nbsp;<i>y</i>) in Cartesian coordinates.
     */
    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    /**
     * Returns the cube root of a {@code double} value. For positive finite {@code x}, {@code cbrt(-x) ==
     * -cbrt(x)}; that is, the cube root of a negative value is the negative of the cube root of that value's magnitude.
     * <p/>
     * Special cases:
     * <p/>
     * <ul>
     * <p/>
     * <li>If the argument is NaN, then the result is NaN.
     * <p/>
     * <li>If the argument is infinite, then the result is an infinity with the same sign as the argument.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
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
     * Returns the smallest (closest to negative infinity) {@code double} value that is greater than or equal to the
     * argument and is equal to a mathematical integer. Special cases:
     * <ul>
     * <li>If the argument value is already equal to a mathematical integer, then the result is the same as the
     * argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the
     * argument.
     * <li>If the argument value is less than zero but greater than -1.0, then the result is negative zero.
     * </ul>
     * Note that the value of {@code Math.ceil(x)} is exactly the value of {@code -Math.floor(-x)}.
     *
     * @param a a value.
     * @return the smallest (closest to negative infinity) floating-point value that is greater than or equal to the
     *         argument and is equal to a mathematical integer.
     */
    public static double ceil(double a) {
        return Math.ceil(a);
    }

    /**
     * Returns the first floating-point argument with the sign of the second floating-point argument. Note that unlike
     * the {@link StrictMath#copySign(double, double) StrictMath.copySign} method, this method does not require NaN
     * {@code sign} arguments to be treated as positive values; implementations are permitted to treat some NaN
     * arguments as positive and other NaN arguments as negative to allow greater performance.
     *
     * @param magnitude the parameter providing the magnitude of the result
     * @param sign the parameter providing the sign of the result
     * @return a value with the magnitude of {@code magnitude} and the sign of {@code sign}.
     */
    public static double copySign(double magnitude, double sign) {
        return Math.copySign(magnitude, sign);
    }

    /**
     * Returns the first floating-point argument with the sign of the second floating-point argument. Note that unlike
     * the {@link StrictMath#copySign(float, float) StrictMath.copySign} method, this method does not require NaN
     * {@code sign} arguments to be treated as positive values; implementations are permitted to treat some NaN
     * arguments as positive and other NaN arguments as negative to allow greater performance.
     *
     * @param magnitude the parameter providing the magnitude of the result
     * @param sign the parameter providing the sign of the result
     * @return a value with the magnitude of {@code magnitude} and the sign of {@code sign}.
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
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a an angle, in radians.
     * @return the cosine of the argument.
     */
    public static double cos(double a) {
        return Math.cos(a);
    }

    /**
     * Returns the hyperbolic cosine of a {@code double} value. The hyperbolic cosine of <i>x</i> is defined to be
     * (<i>e<sup>x</sup>&nbsp;+&nbsp;e<sup>-x</sup></i>)/2 where <i>e</i> is {@linkplain Math#E Euler's number}.
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
     * Returns Euler's number <i>e</i> raised to the power of a {@code double} value. Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive infinity.
     * <li>If the argument is negative infinity, then the result is positive zero.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a the exponent to raise <i>e</i> to.
     * @return the value <i>e</i><sup>{@code a}</sup>, where <i>e</i> is the base of the natural logarithms.
     */
    public static double exp(double a) {
        return Math.exp(a);
    }

    /**
     * Returns <i>e</i><sup>x</sup>&nbsp;-1. Note that for values of <i>x</i> near 0, the exact sum of
     * {@code expm1(x)}&nbsp;+&nbsp;1 is much closer to the true result of <i>e</i><sup>x</sup> than {@code exp(x)}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, then the result is positive infinity.
     * <p/>
     * <li>If the argument is negative infinity, then the result is -1.0.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic. The result of
     * {@code expm1} for any finite input must be greater than or equal to {@code -1.0}. Note that once the exact result
     * of <i>e</i><sup>{@code x}</sup>&nbsp;-&nbsp;1 is within 1/2 ulp of the limit value -1, {@code -1.0} should be
     * returned.
     *
     * @param x the exponent to raise <i>e</i> to in the computation of <i>e</i><sup>{@code x}</sup>&nbsp;-1.
     * @return the value <i>e</i><sup>{@code x}</sup>&nbsp;-&nbsp;1.
     */
    public static double expm1(double x) {
        return Math.expm1(x);
    }

    /**
     * Returns the largest (closest to positive infinity) {@code double} value that is less than or equal to the
     * argument and is equal to a mathematical integer. Special cases:
     * <ul>
     * <li>If the argument value is already equal to a mathematical integer, then the result is the same as the
     * argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the
     * argument.
     * </ul>
     *
     * @param a a value.
     * @return the largest (closest to positive infinity) floating-point value that less than or equal to the argument
     *         and is equal to a mathematical integer.
     */
    public static double floor(double a) {
        return Math.floor(a);
    }

    /**
     * Returns the unbiased exponent used in the representation of a {@code double}. Special cases:
     * <p/>
     * <ul>
     * <li>If the argument is NaN or infinite, then the result is {@link Double#MAX_EXPONENT} + 1.
     * <li>If the argument is zero or subnormal, then the result is {@link Double#MIN_EXPONENT} -1.
     * </ul>
     *
     * @param d a {@code double} value
     * @return the unbiased exponent of the argument
     */
    public static int getExponent(double d) {
        return Math.getExponent(d);
    }

    /**
     * Returns the unbiased exponent used in the representation of a {@code float}. Special cases:
     * <p/>
     * <ul>
     * <li>If the argument is NaN or infinite, then the result is {@link Float#MAX_EXPONENT} + 1.
     * <li>If the argument is zero or subnormal, then the result is {@link Float#MIN_EXPONENT} -1.
     * </ul>
     *
     * @param f a {@code float} value
     * @return the unbiased exponent of the argument
     */
    public static int getExponent(float f) {
        return Math.getExponent(f);
    }

    /**
     * Returns sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>) without intermediate overflow or underflow.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <p/>
     * <li>If either argument is infinite, then the result is positive infinity.
     * <p/>
     * <li>If either argument is NaN and neither argument is infinite, then the result is NaN.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. If one parameter is held constant, the results must
     * be semi-monotonic in the other parameter.
     *
     * @param x a value
     * @param y a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>) without intermediate overflow or underflow
     */
    public static double getExponent(double x, double y) {
        return Math.hypot(x, y);
    }

    /**
     * Computes the remainder operation on two arguments as prescribed by the IEEE 754 standard. The remainder value is
     * mathematically equal to <code>f1&nbsp;-&nbsp;f2</code>&nbsp;&times;&nbsp;<i>n</i>, where <i>n</i> is the
     * mathematical integer closest to the exact mathematical value of the quotient {@code f1/f2}, and if two
     * mathematical integers are equally close to {@code f1/f2}, then <i>n</i> is the integer that is even. If the
     * remainder is zero, its sign is the same as the sign of the first argument. Special cases:
     * <ul>
     * <li>If either argument is NaN, or the first argument is infinite, or the second argument is positive zero or
     * negative zero, then the result is NaN.
     * <li>If the first argument is finite and the second argument is infinite, then the result is the same as the first
     * argument.
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
     * Returns the natural logarithm (base <i>e</i>) of a {@code double} value. Special cases:
     * <ul>
     * <li>If the argument is NaN or less than zero, then the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive infinity.
     * <li>If the argument is positive zero or negative zero, then the result is negative infinity.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
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
     * <li>If the argument is positive infinity, then the result is positive infinity.
     * <li>If the argument is positive zero or negative zero, then the result is negative infinity.
     * <li>If the argument is equal to 10<sup><i>n</i></sup> for integer <i>n</i>, then the result is <i>n</i>.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a a value
     * @return the base 10 logarithm of {@code a}.
     */
    public static double log10(double a) {
        return Math.log10(a);
    }

    /**
     * Returns the natural logarithm of the sum of the argument and 1. Note that for small values {@code x}, the result
     * of {@code log1p(x)} is much closer to the true result of ln(1 + {@code x}) than the floating-point evaluation of
     * {@code log(1.0+x)}.
     * <p/>
     * <p/>
     * Special cases:
     * <p/>
     * <ul>
     * <p/>
     * <li>If the argument is NaN or less than -1, then the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, then the result is positive infinity.
     * <p/>
     * <li>If the argument is negative one, then the result is negative infinity.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param x a value
     * @return the value ln({@code x}&nbsp;+&nbsp;1), the natural log of {@code x}&nbsp;+&nbsp;1
     */
    public static double log1p(double x) {
        return Math.log1p(x);
    }

    /**
     * Returns the floating-point number adjacent to the first argument in the direction of the second argument. If both
     * arguments compare as equal the second argument is returned.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If either argument is a NaN, then NaN is returned.
     * <p/>
     * <li>If both arguments are signed zeros, {@code direction} is returned unchanged (as implied by the requirement of
     * returning the second argument if the arguments compare as equal).
     * <p/>
     * <li>If {@code start} is &plusmn;{@link Double#MIN_VALUE} and {@code direction} has a value such that the result
     * should have a smaller magnitude, then a zero with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is infinite and {@code direction} has a value such that the result should have a smaller
     * magnitude, {@link Double#MAX_VALUE} with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is equal to &plusmn; {@link Double#MAX_VALUE} and {@code direction} has a value such that
     * the result should have a larger magnitude, an infinity with same sign as {@code start} is returned.
     * </ul>
     *
     * @param start starting floating-point value
     * @param direction value indicating which of {@code start}'s neighbors or {@code start} should be returned
     * @return The floating-point number adjacent to {@code start} in the direction of {@code direction}.
     */
    public static double nextAfter(double start, double direction) {
        return Math.nextAfter(start, direction);
    }

    /**
     * Returns the floating-point number adjacent to the first argument in the direction of the second argument. If both
     * arguments compare as equal a value equivalent to the second argument is returned.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If either argument is a NaN, then NaN is returned.
     * <p/>
     * <li>If both arguments are signed zeros, a value equivalent to {@code direction} is returned.
     * <p/>
     * <li>If {@code start} is &plusmn;{@link Float#MIN_VALUE} and {@code direction} has a value such that the result
     * should have a smaller magnitude, then a zero with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is infinite and {@code direction} has a value such that the result should have a smaller
     * magnitude, {@link Float#MAX_VALUE} with the same sign as {@code start} is returned.
     * <p/>
     * <li>If {@code start} is equal to &plusmn; {@link Float#MAX_VALUE} and {@code direction} has a value such that the
     * result should have a larger magnitude, an infinity with same sign as {@code start} is returned.
     * </ul>
     *
     * @param start starting floating-point value
     * @param direction value indicating which of {@code start}'s neighbors or {@code start} should be returned
     * @return The floating-point number adjacent to {@code start} in the direction of {@code direction}.
     */
    public static float nextAfter(float start, float direction) {
        return Math.nextAfter(start, direction);
    }

    /**
     * Returns the floating-point value adjacent to {@code f} in the direction of positive infinity. This method is
     * semantically equivalent to {@code nextAfter(f,
     * Float.POSITIVE_INFINITY)}; however, a {@code nextUp} implementation may run faster than its equivalent
     * {@code nextAfter} call.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, the result is positive infinity.
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
     * Returns the floating-point value adjacent to {@code d} in the direction of positive infinity. This method is
     * semantically equivalent to {@code nextAfter(d,
     * Double.POSITIVE_INFINITY)}; however, a {@code nextUp} implementation may run faster than its equivalent
     * {@code nextAfter} call.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, the result is NaN.
     * <p/>
     * <li>If the argument is positive infinity, the result is positive infinity.
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
     * Returns the value of the first argument raised to the power of the second argument. Special cases:
     * <p/>
     * <ul>
     * <li>If the second argument is positive or negative zero, then the result is 1.0.
     * <li>If the second argument is 1.0, then the result is the same as the first argument.
     * <li>If the second argument is NaN, then the result is NaN.
     * <li>If the first argument is NaN and the second argument is nonzero, then the result is NaN.
     * <p/>
     * <li>If
     * <ul>
     * <li>the absolute value of the first argument is greater than 1 and the second argument is positive infinity, or
     * <li>the absolute value of the first argument is less than 1 and the second argument is negative infinity,
     * </ul>
     * then the result is positive infinity.
     * <p/>
     * <li>If
     * <ul>
     * <li>the absolute value of the first argument is greater than 1 and the second argument is negative infinity, or
     * <li>the absolute value of the first argument is less than 1 and the second argument is positive infinity,
     * </ul>
     * then the result is positive zero.
     * <p/>
     * <li>If the absolute value of the first argument equals 1 and the second argument is infinite, then the result is
     * NaN.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is positive zero and the second argument is greater than zero, or
     * <li>the first argument is positive infinity and the second argument is less than zero,
     * </ul>
     * then the result is positive zero.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is positive zero and the second argument is less than zero, or
     * <li>the first argument is positive infinity and the second argument is greater than zero,
     * </ul>
     * then the result is positive infinity.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is greater than zero but not a finite odd
     * integer, or
     * <li>the first argument is negative infinity and the second argument is less than zero but not a finite odd
     * integer,
     * </ul>
     * then the result is positive zero.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is a positive finite odd integer, or
     * <li>the first argument is negative infinity and the second argument is a negative finite odd integer,
     * </ul>
     * then the result is negative zero.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is less than zero but not a finite odd integer,
     * or
     * <li>the first argument is negative infinity and the second argument is greater than zero but not a finite odd
     * integer,
     * </ul>
     * then the result is positive infinity.
     * <p/>
     * <li>If
     * <ul>
     * <li>the first argument is negative zero and the second argument is a negative finite odd integer, or
     * <li>the first argument is negative infinity and the second argument is a positive finite odd integer,
     * </ul>
     * then the result is negative infinity.
     * <p/>
     * <li>If the first argument is finite and less than zero
     * <ul>
     * <li>if the second argument is a finite even integer, the result is equal to the result of raising the absolute
     * value of the first argument to the power of the second argument
     * <p/>
     * <li>if the second argument is a finite odd integer, the result is equal to the negative of the result of raising
     * the absolute value of the first argument to the power of the second argument
     * <p/>
     * <li>if the second argument is finite and not an integer, then the result is NaN.
     * </ul>
     * <p/>
     * <li>If both arguments are integers, then the result is exactly equal to the mathematical result of raising the
     * first argument to the power of the second argument if that result can in fact be represented exactly as a
     * {@code double} value.
     * </ul>
     * <p/>
     * <p/>
     * (In the foregoing descriptions, a floating-point value is considered to be an integer if and only if it is finite
     * and a fixed point of the method {@link #ceil ceil} or, equivalently, a fixed point of the method {@link #floor
     * floor}. A value is a fixed point of a one-argument method if and only if the result of applying the method to the
     * value is equal to the value.)
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
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

    public static double pow(Double a, Double b) {
        if (a == null) {
            return b == null ? null : 0;
        } else if (b == null) {
            return a;
        }
        return Math.pow(a, b);
    }

    // ---------------------------------------

    /**
     * Returns a {@code double} value with a positive sign, greater than or equal to {@code 0.0} and less than
     * {@code 1.0}. Returned values are chosen pseudorandomly with (approximately) uniform distribution from that range.
     * <p/>
     * <p/>
     * When this method is first called, it creates a single new pseudorandom-number generator, exactly as if by the
     * expression
     * <p/>
     * <blockquote>{@code new java.util.Random()}</blockquote>
     * <p/>
     * This new pseudorandom-number generator is used thereafter for all calls to this method and is used nowhere else.
     * <p/>
     * <p/>
     * This method is properly synchronized to allow correct use by more than one thread. However, if many threads need
     * to generate pseudorandom numbers at a great rate, it may reduce contention for each thread to have its own
     * pseudorandom-number generator.
     *
     * @return a pseudorandom {@code double} greater than or equal to {@code 0.0} and less than {@code 1.0}.
     */
    public static double random() {
        return Math.random();
    }

    /**
     * Returns the {@code double} value that is closest in value to the argument and is equal to a mathematical integer.
     * If two {@code double} values that are mathematical integers are equally close, the result is the integer value
     * that is even. Special cases:
     * <ul>
     * <li>If the argument value is already equal to a mathematical integer, then the result is the same as the
     * argument.
     * <li>If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the
     * argument.
     * </ul>
     *
     * @param a a {@code double} value.
     * @return the closest floating-point value to {@code a} that is equal to a mathematical integer.
     */
    public static double rint(double a) {
        return Math.rint(a);
    }

    /**
     * Return {@code d} &times; 2<sup>{@code scaleFactor}</sup> rounded as if performed by a single correctly rounded
     * floating-point multiply to a member of the double value set. See the Java Language Specification for a discussion
     * of floating-point value sets. If the exponent of the result is between {@link Double#MIN_EXPONENT} and
     * {@link Double#MAX_EXPONENT}, the answer is calculated exactly. If the exponent of the result would be larger than
     * {@code Double.MAX_EXPONENT}, an infinity is returned. Note that if the result is subnormal, precision may be
     * lost; that is, when {@code scalb(x, n)} is subnormal, {@code scalb(scalb(x, n), -n)} may not equal <i>x</i>. When
     * the result is non-NaN, the result has the same sign as {@code d}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the first argument is NaN, NaN is returned.
     * <li>If the first argument is infinite, then an infinity of the same sign is returned.
     * <li>If the first argument is zero, then a zero of the same sign is returned.
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
     * Return {@code f} &times; 2<sup>{@code scaleFactor}</sup> rounded as if performed by a single correctly rounded
     * floating-point multiply to a member of the float value set. See the Java Language Specification for a discussion
     * of floating-point value sets. If the exponent of the result is between {@link Float#MIN_EXPONENT} and
     * {@link Float#MAX_EXPONENT}, the answer is calculated exactly. If the exponent of the result would be larger than
     * {@code Float.MAX_EXPONENT}, an infinity is returned. Note that if the result is subnormal, precision may be lost;
     * that is, when {@code scalb(x, n)} is subnormal, {@code scalb(scalb(x, n), -n)} may not equal <i>x</i>. When the
     * result is non-NaN, the result has the same sign as {@code f}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <li>If the first argument is NaN, NaN is returned.
     * <li>If the first argument is infinite, then an infinity of the same sign is returned.
     * <li>If the first argument is zero, then a zero of the same sign is returned.
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
     * Returns the signum function of the argument; zero if the argument is zero, 1.0 if the argument is greater than
     * zero, -1.0 if the argument is less than zero.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive zero or negative zero, then the result is the same as the argument.
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
     * Returns the signum function of the argument; zero if the argument is zero, 1.0f if the argument is greater than
     * zero, -1.0f if the argument is less than zero.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive zero or negative zero, then the result is the same as the argument.
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
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a an angle, in radians.
     * @return the sine of the argument.
     */
    public static double sin(double a) {
        return Math.sin(a);
    }

    /**
     * Returns the hyperbolic sine of a {@code double} value. The hyperbolic sine of <i>x</i> is defined to be
     * (<i>e<sup>x</sup>&nbsp;-&nbsp;e<sup>-x</sup></i>)/2 where <i>e</i> is {@linkplain Math#E Euler's number}.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <p/>
     * <li>If the argument is NaN, then the result is NaN.
     * <p/>
     * <li>If the argument is infinite, then the result is an infinity with the same sign as the argument.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
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
     * Returns the correctly rounded positive square root of a {@code double} value. Special cases:
     * <ul>
     * <li>If the argument is NaN or less than zero, then the result is NaN.
     * <li>If the argument is positive infinity, then the result is positive infinity.
     * <li>If the argument is positive zero or negative zero, then the result is the same as the argument.
     * </ul>
     * Otherwise, the result is the {@code double} value closest to the true mathematical square root of the argument
     * value.
     *
     * @param a a value.
     * @return the positive square root of {@code a}. If the argument is NaN or less than zero, the result is NaN.
     */
    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    /**
     * Returns the trigonometric tangent of an angle. Special cases:
     * <ul>
     * <li>If the argument is NaN or an infinity, then the result is NaN.
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 1 ulp of the exact result. Results must be semi-monotonic.
     *
     * @param a an angle, in radians.
     * @return the tangent of the argument.
     */
    public static double tan(double a) {
        return Math.tan(a);
    }

    /**
     * Returns the hyperbolic tangent of a {@code double} value. The hyperbolic tangent of <i>x</i> is defined to be
     * (<i>e<sup>x</sup>&nbsp;-&nbsp;e<sup>- x</sup></i>)/(<i>e<sup>x</sup>&nbsp;+&nbsp;e<sup>-x</sup></i>), in other
     * words, {@linkplain Math#sinh sinh(<i>x</i>)}/{@linkplain Math#cosh cosh(<i>x</i>)}. Note that the absolute value
     * of the exact tanh is always less than 1.
     * <p/>
     * <p/>
     * Special cases:
     * <ul>
     * <p/>
     * <li>If the argument is NaN, then the result is NaN.
     * <p/>
     * <li>If the argument is zero, then the result is a zero with the same sign as the argument.
     * <p/>
     * <li>If the argument is positive infinity, then the result is {@code +1.0}.
     * <p/>
     * <li>If the argument is negative infinity, then the result is {@code -1.0}.
     * <p/>
     * </ul>
     * <p/>
     * <p/>
     * The computed result must be within 2.5 ulps of the exact result. The result of {@code tanh} for any finite input
     * must have an absolute value less than or equal to 1. Note that once the exact result of tanh is within 1/2 of an
     * ulp of the limit value of &plusmn;1, correctly signed &plusmn;{@code 1.0} should be returned.
     *
     * @param x The number whose hyperbolic tangent is to be returned.
     * @return The hyperbolic tangent of {@code x}.
     */
    public static double tanh(double x) {
        return Math.tanh(x);
    }

    /**
     * Converts an angle measured in radians to an approximately equivalent angle measured in degrees. The conversion
     * from radians to degrees is generally inexact; users should <i>not</i> expect {@code cos(toRadians(90.0))} to
     * exactly equal {@code 0.0}.
     *
     * @param angrad an angle, in radians
     * @return the measurement of the angle {@code angrad} in degrees.
     */
    public static double toDegrees(double angrad) {
        return Math.toDegrees(angrad);
    }

    /**
     * Converts an angle measured in degrees to an approximately equivalent angle measured in radians. The conversion
     * from degrees to radians is generally inexact.
     *
     * @param angdeg an angle, in degrees
     * @return the measurement of the angle {@code angdeg} in radians.
     */
    public static double toRadians(double angdeg) {
        return Math.toRadians(angdeg);
    }

    /**
     * Returns the size of an ulp of the argument. An ulp of a {@code double} value is the positive distance between
     * this floating-point value and the {@code double} value next larger in magnitude. Note that for non-NaN <i>x</i>,
     * <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive or negative infinity, then the result is positive infinity.
     * <li>If the argument is positive or negative zero, then the result is {@code Double.MIN_VALUE}.
     * <li>If the argument is &plusmn;{@code Double.MAX_VALUE}, then the result is equal to 2<sup>971</sup>.
     * </ul>
     *
     * @param d the floating-point value whose ulp is to be returned
     * @return the size of an ulp of the argument
     */
    public static double ulp(double d) {
        return Math.ulp(d);
    }

    /**
     * Returns the size of an ulp of the argument. An ulp of a {@code float} value is the positive distance between this
     * floating-point value and the {@code float} value next larger in magnitude. Note that for non-NaN <i>x</i>,
     * <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
     * <p/>
     * <p/>
     * Special Cases:
     * <ul>
     * <li>If the argument is NaN, then the result is NaN.
     * <li>If the argument is positive or negative infinity, then the result is positive infinity.
     * <li>If the argument is positive or negative zero, then the result is {@code Float.MIN_VALUE}.
     * <li>If the argument is &plusmn;{@code Float.MAX_VALUE}, then the result is equal to 2<sup>104</sup>.
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
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, true)          = [true]
     * ArrayUtils.add([true], false)       = [true, false]
     * ArrayUtils.add([true, false], true) = [true, false, true]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static boolean[] add(boolean[] array, boolean element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static boolean[] add(boolean[] array, int index, boolean element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static byte[] add(byte[] array, byte element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static byte[] add(byte[] array, int index, byte element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, '0')       = ['0']
     * ArrayUtils.add(['1'], '0')      = ['1', '0']
     * ArrayUtils.add(['1', '0'], '1') = ['1', '0', '1']
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static char[] add(char[] array, char element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static char[] add(char[] array, int index, char element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static double[] add(double[] array, double element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static double[] add(double[] array, int index, double element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static float[] add(float[] array, float element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static float[] add(float[] array, int index, float element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static int[] add(int[] array, int element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static int[] add(int[] array, int index, int element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static long[] add(long[] array, int index, long element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     */
    public static long[] add(long[] array, long element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */

    public static <T> T[] add(T[] array, int index, T element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element, unless the element itself is null, in which case the return type is Object[]
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
     * @return A new array containing the existing elements plus the new element The returned array type will be that of
     *         the input array (unless null), in which case it will have the same type as the element.
     */
    public static <T> T[] add(T[] array, T element) {
        return ArrayUtils.add(array, element);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static short[] add(short[] array, int index, short element) {
        return ArrayUtils.add(array, index, element);
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given element in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * ArrayUtils.add(null, 0)   = [0]
     * ArrayUtils.add([1], 0)    = [1, 0]
     * ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array the array to copy and add the element to, may be <code>null</code>
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
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static <T> T[] addIgnoreNull(T[] array, int index, T element) {
        if (element != null) {
            return ArrayUtils.add(array, index, element);
        }
        return array;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static <T> T[] addIgnoreNull(T[] array, T element) {
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * @param array1 the first array whose elements are added to the new array, may be <code>null</code>
     * @param array2 the second array whose elements are added to the new array, may be <code>null</code>
     * @return The new array, <code>null</code> if both arrays are <code>null</code>. The type of the new array is the
     *         type of the first array, unless the first array is null, in which case the type is the same as the second
     *         array.
     * @throws IllegalArgumentException if the array types are incompatible
     */
    public static <T> T[] addAll(T[] array1, T[] array2) {
        return ArrayUtils.addAll(array1, array2);
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code> followed by all of the elements
     * <code>array2</code>. When an array is returned, it is always a new array.
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
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static boolean[] remove(boolean[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static byte[] remove(byte[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static char[] remove(char[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static double[] remove(double[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static float[] remove(float[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static int[] remove(int[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static long[] remove(long[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
     */
    public static <T> T[] remove(T[] array, int index) {
        return ArrayUtils.remove(array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array. All subsequent elements are shifted to
     * the left (substracts one from their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException will be thrown, because in that case no
     * valid index can be specified.
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
     * @param array the array to remove the element from, may not be <code>null</code>
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length), or if the
     *             array is <code>null</code>.
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
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static boolean[] removeElement(boolean[] array, boolean element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static byte[] removeElement(byte[] array, byte element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static char[] removeElement(char[] array, char element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static double[] removeElement(double[] array, double element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static float[] removeElement(float[] array, float element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static int[] removeElement(int[] array, int element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static long[] removeElement(long[] array, long element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array.
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
     * @param array the array to remove the element from, may be <code>null</code>
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first occurrence of the specified element.
     */
    public static short[] removeElement(short[] array, short element) {
        return ArrayUtils.removeElement(array, element);
    }

    /**
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array. <br />
     * <br />
     * <p/>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array. <br />
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
    public static <T> T[] removeElement(T[] array, T element) {
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

    // <<< startsWith and endsWith for Strings >>>

    // <<< subString >>>

    // <<< removeStart and removeEnd >>>

    /*
     * into the return statement the full path of StringUtils should be written if we write StringUtils.removeStart(str,
     * remove); WebStudio won't work correctly;
     */

    // <<< lowerCase and upperCase functions >>>

    // <<< replace functions for Strings >>>

    @AutoCastReturnType
    public static Object[] flatten(@ReturnType Object... data) {
        if (data == null) {
            return null;
        }
        List<Object> values = new ArrayList<>();
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

    public static Class<?> getCommonSuperClass(Class<?> classA, Class<?> classB) {
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

    @AutoCastReturnType(RulesUtilsGetValuesAutoCastFactory.class)
    public static Object getValues(DomainOpenClass clazz) {
        IDomain<?> domain = clazz.getDomain();
        int size = 0;
        for (Object item : domain) {
            size++;
        }

        Class<?> type = clazz.getInstanceClass();
        Object result = Array.newInstance(type, size);
        int i = 0;
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

    @SuppressWarnings("unchecked")
    public static <T> T copy(T origin) {
        // Create new instance every time because of the issue with updating a module in WebStudio.
        // It is needed to investigate class loading/unloading mechanism.
        if (origin == null) {
            return null;
        }

        Class<?> clazz = origin.getClass();
        if (clazz.isArray() && Serializable.class
            .isAssignableFrom(clazz.getComponentType()) || !clazz.isArray() && origin instanceof Serializable) {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
                oos = new ObjectOutputStream(baos);
                oos.writeObject(origin);

                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                ois = new ClassLoaderObjectInputStream(Thread.currentThread().getContextClassLoader(), bais);
                return (T) ois.readObject();
            } catch (Exception unused) {
                // OK lets use cloner
            } finally {
                IOUtils.closeQuietly(oos);
                IOUtils.closeQuietly(ois);
            }
        }
        // FIXME: Needless memory consumption - no needs to create 'cloner' instance every time.
        return new OpenLArgumentsCloner().deepClone(origin);
    }
}
