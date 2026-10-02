package org.openl.rules.dt.algorithm.evaluator;

import java.util.Comparator;

import org.openl.rules.helpers.NumberUtils;

/**
 * Orders the floating-point values of a decision table condition, so that an index finds the rules of a value.
 *
 * <p>Two finite numbers are equal when they differ by at most one ulp of the first number, as the {@code ==}
 * operator compares them.
 *
 * <p>{@code NaN} equals only {@code NaN}, and an infinity equals only the infinity of the same sign. Neither equals a
 * finite number.
 */
public class FloatTypeComparator implements Comparator<Object> {
    private static final FloatTypeComparator INSTANCE = new FloatTypeComparator();

    private FloatTypeComparator() {
    }

    public static FloatTypeComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(Object o1, Object o2) {
        var d1 = NumberUtils.convertToDouble(o1);
        var d2 = NumberUtils.convertToDouble(o2);
        if (Double.isFinite(d1) && Double.isFinite(d2) && Double.compare(Math.abs(d1 - d2), Math.ulp(d1)) <= 0) {
            return 0;
        }
        return Double.compare(d1, d2);
    }
}
