package org.openl.rules.dt.type;

import org.openl.rules.helpers.DoubleRange;
import org.openl.util.RangeWithBounds.BoundType;

public class DoubleRangeAdaptor implements IRangeAdaptor<DoubleRange, Double> {

    public Comparable<Double> getMax(DoubleRange range) {
        double max = range.getUpperBound();
        if (max != Double.POSITIVE_INFINITY && range.getUpperBoundType() != BoundType.INCLUDING) {
            max += Math.ulp(max);
        }
        return max;
    }

    public Comparable<Double> getMin(DoubleRange range) {
        double min = range.getLowerBound();
        if (range.getLowerBoundType() == BoundType.EXCLUDING) {
            min += Math.ulp(min);
        }
        return min;
    }
}
