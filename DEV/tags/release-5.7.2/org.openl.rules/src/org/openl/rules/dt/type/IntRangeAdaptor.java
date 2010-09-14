package org.openl.rules.dt.type;

import org.openl.rules.helpers.IntRange;

public class IntRangeAdaptor implements IRangeAdaptor<IntRange, Long> {

    /**
     * {@inheritDoc}
     */
    public Long getMax(IntRange range) {

        int max = range.getMax();

        if (max != Integer.MAX_VALUE) {
            max = max + 1;
        }

        return new Long(max);
    }

    /**
     * {@inheritDoc}
     */
    public Long getMin(IntRange range) {
        return new Long(range.getMin());
    }

}
