package org.openl.rules.dt.type;

import org.openl.rules.helpers.IntRange;

public final class DoubleRangeForIntRangeAdaptor implements IRangeAdaptor<IntRange, Double> {
    private static final DoubleRangeForIntRangeAdaptor INSTANCE = new DoubleRangeForIntRangeAdaptor();

    private DoubleRangeForIntRangeAdaptor() {
    }

    public static IRangeAdaptor<IntRange, Double> getInstance() {
        return INSTANCE;
    }

    @Override
    public Double getMax(IntRange range) {
        if (range == null) {
            return null;
        }

        double max = range.getMax();
        if (max != Double.POSITIVE_INFINITY) {
            // the max should be moved to the right,
            // to ensure that range.getUpperBound() will get to the interval
            //
            max += Math.ulp(max);
        }
        return max;
    }

    @Override
    public Double getMin(IntRange range) {
        if (range == null) {
            return null;
        }
        return Double.valueOf(range.getMin());
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
