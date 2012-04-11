package org.openl.rules.dt.type;

import org.openl.rules.helpers.IntRange;

public class IntRangeAdaptor implements IRangeAdaptor<IntRange, Integer> {

    /**
     * {@inheritDoc}
     */
    public Integer getMax(IntRange range) {

        int max = range.getMax();

        if (max != Integer.MAX_VALUE) {
            max = max + 1;
        }

        return Integer.valueOf(max);
    }

    /**
     * {@inheritDoc}
     */
    public Integer getMin(IntRange range) {
        return Integer.valueOf(range.getMin());
    }

}
