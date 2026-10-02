package org.openl.rules.dt.type;

import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.range.Range;

public final class DoubleRangeAdaptor implements IRangeAdaptor<DoubleRange, Double> {
    private static final DoubleRangeAdaptor INSTANCE = new DoubleRangeAdaptor();

    private DoubleRangeAdaptor() {
    }

    public static IRangeAdaptor<DoubleRange, Double> getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the first number above the range.
     *
     * <p>For a range that holds {@code Infinity}, such as {@code > 10}, the result is {@code NaN}. {@code NaN} is the
     * only {@code Double} ordered above {@code Infinity}, so {@code Infinity} falls inside the range, while
     * {@code NaN} itself stays outside every range.
     */
    @Override
    public Double getMax(DoubleRange range) {
        if (range == null) {
            return null;
        }

        var max = range.getUpperBound();
        if (range.getType().right == Range.Bound.OPEN) {
            return max;
        }
        return max == Double.POSITIVE_INFINITY ? Double.NaN : Math.nextUp(max);
    }

    /**
     * Returns the first number of the range.
     */
    @Override
    public Double getMin(DoubleRange range) {
        if (range == null) {
            return null;
        }

        var min = range.getLowerBound();
        return range.getType().left == Range.Bound.OPEN ? Math.nextUp(min) : min;
    }

    @Override
    public Double adaptValueType(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).doubleValue();
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }

}
