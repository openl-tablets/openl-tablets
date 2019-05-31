package org.openl.rules.dt.type;

import org.openl.rules.helpers.IntRange;

public final class IntRangeAdaptor implements IRangeAdaptor<IntRange, Long> {
    private static final IntRangeAdaptor INSTANCE = new IntRangeAdaptor();

    private IntRangeAdaptor() {
    }

    public static IRangeAdaptor<IntRange, Long> getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getMax(IntRange range) {
        if (range == null) {
            return null;
        }

        long max = range.getMax();

        if (max != Long.MAX_VALUE) {
            max = max + 1;
        }

        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getMin(IntRange range) {
        if (range == null) {
            return null;
        }

        return range.getMin();
    }

    @Override
    public Long adaptValueType(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }

}
