/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import org.openl.OpenL;
import org.openl.SourceType;
import org.openl.engine.OpenLManager;
import org.openl.syntax.impl.StringSourceCodeModule;
import org.openl.util.RangeWithBounds;

/**
 * The <code>DoubleRange</code> class stores range of floats. Examples :
 * "1.2-3", "2 .. 4", "123.456 ... 1000.00001" (Important: using of ".." and
 * "..." requires spaces between numbers and separator).
 */
public class DoubleRange implements INumberRange {
    double lowerBound = Double.MIN_VALUE;

    double upperBound = Double.MAX_VALUE;

    public DoubleRange(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (lowerBound > upperBound) {
            throw new RuntimeException(upperBound + " must be more or equal than " + lowerBound);
        }
    }

    public DoubleRange(String s) {
        // TODO: Correct tokenizing in grammar.
        OpenL openl = OpenL.getInstance("org.openl.j");
        RangeWithBounds res = (RangeWithBounds) OpenLManager.run(openl, new StringSourceCodeModule(s, null),
                SourceType.DOUBLE_RANGE);
        lowerBound = res.getMin().doubleValue();
        upperBound = res.getMax().doubleValue();
    }
    public boolean contains(double x) {
        return lowerBound <= x && x <= upperBound;
    }

    public boolean contains(DoubleRange range) {
        return lowerBound <= range.lowerBound && range.upperBound <= upperBound;
    }

    public boolean containsNumber(Number num) {
        return lowerBound <= num.doubleValue() && num.doubleValue() <= upperBound;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof DoubleRange)) {
            return false;
        }
        DoubleRange other = (DoubleRange) obj;
        return other.lowerBound == lowerBound && other.upperBound == upperBound;
    }

    /**
     * @return Returns the lowerBound.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * @return Returns the upperBound.
     */
    public double getUpperBound() {
        return upperBound;
    }

    public DoubleRange intersect(DoubleRange range) {
        double maxLowerBound = Math.max(lowerBound, range.lowerBound);
        double minUpperBound = Math.min(upperBound, range.upperBound);
        return maxLowerBound > minUpperBound ? null : new DoubleRange(maxLowerBound, minUpperBound);
    }

    /**
     * @param lowerBound The lowerBound to set.
     */
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @param upperBound The upperBound to set.
     */
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }
}
