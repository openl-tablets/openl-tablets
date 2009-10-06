package org.openl.rules.liveexcel.ranges;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

/**
 * Eval for numeric ranges.
 * 
 * @author PUdalau
 */
public interface RangeEval extends ValueEval {
    /**
     * @return Lower bound of range.
     */
    double getLowerBound();

    /**
     * @return Upper bound of range.
     */
    double getUpperBound();

    /**
     * @return Type of lower bound.
     */
    BoundType getLowerBoundType();

    /**
     * @return Type of upper bound.
     */
    BoundType getUpperBoundType();

    /**
     * Checks if number belongs to interval.
     * 
     * @param number Number to check.
     * @return <code>true</code> if number belong to range.
     */
    boolean contains(double number);
}
