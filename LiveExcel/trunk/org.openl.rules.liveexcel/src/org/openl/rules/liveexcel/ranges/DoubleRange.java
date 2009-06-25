package org.openl.rules.liveexcel.ranges;

public class DoubleRange implements RangeEval {
    private double lowerBound = Double.NEGATIVE_INFINITY;
    private double upperBound = Double.POSITIVE_INFINITY;
    private BoundType lowerBoundType = BoundType.OPEN;
    private BoundType upperBoundType = BoundType.OPEN;

    public DoubleRange(double lowerBound, double upperBound, BoundType lowerBoundType, BoundType upperBoundType) {
        if (lowerBound > upperBound) {
            throw new RuntimeException("Upper bound[=" + upperBound + "] must be more or equal than lower bound[="
                    + lowerBound + "]");
        }
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerBoundType = lowerBoundType;
        this.upperBoundType = upperBoundType;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public BoundType getLowerBoundType() {
        return lowerBoundType;
    }

    public void setLowerBoundType(BoundType lowerBoundType) {
        this.lowerBoundType = lowerBoundType;
    }

    public BoundType getUpperBoundType() {
        return upperBoundType;
    }

    public void setUpperBoundType(BoundType upperBoundType) {
        this.upperBoundType = upperBoundType;
    }

    public boolean contains(double number) {
        if (number < upperBound && number > lowerBound) {
            return true;
        }
        if (lowerBoundType == BoundType.CLOSED && Double.compare(lowerBound, number) == 0) {
            return true;
        }
        if (upperBoundType == BoundType.CLOSED && Double.compare(upperBound, number) == 0) {
            return true;
        }
        return false;
    }

    public boolean contains(DoubleRange range) {
        return this.contains(range.getLowerBound()) && this.contains(range.upperBound);
    }
}
