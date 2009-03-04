/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import org.openl.OpenL;
import org.openl.syntax.impl.StringSourceCodeModule;
import org.openl.util.RangeWithBounds;

/**
 * @author snshor
 */
public class DoubleRange implements INumberRange {
    public DoubleRange(String s) {
        // Spaces between numbers and '-' are mandatory. Example: "1.0 - 2.0" -
        // correct "1.0-2.0" - wrong.
        // TODO: Correct tokenizing in grammar.
        OpenL openl = OpenL.getInstance("org.openl.j");
        RangeWithBounds res = (RangeWithBounds) openl.evaluate(new StringSourceCodeModule(s, null),
                "range.literal.real");
        lowerBound = res.getMin().doubleValue();
        upperBound = res.getMax().doubleValue();
    }

    public DoubleRange(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (lowerBound > upperBound){
            throw new RuntimeException(upperBound + " must be more or equal than " + lowerBound);
        }
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

    double lowerBound = Double.MIN_VALUE;
    double upperBound = Double.MAX_VALUE;

    public boolean contains(double x) {
        return lowerBound <= x && x <= upperBound;
    }

    public boolean containsNumber(Number num) {
        return lowerBound <= num.doubleValue() && num.doubleValue() <= upperBound;
    }

    /**
     * @return Returns the lowerBound.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * @param lowerBound The lowerBound to set.
     */
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @return Returns the upperBound.
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * @param upperBound The upperBound to set.
     */
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public boolean contains(DoubleRange range) {
        return lowerBound <= range.lowerBound && range.upperBound <= upperBound;
    }

    public DoubleRange intersect(DoubleRange range) {
        double maxLowerBound = Math.max(lowerBound, range.lowerBound);
        double minUpperBound = Math.min(upperBound, range.upperBound);
        return maxLowerBound > minUpperBound ? null : new DoubleRange(maxLowerBound, minUpperBound);
    }
}
