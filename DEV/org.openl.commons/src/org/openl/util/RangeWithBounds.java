package org.openl.util;

import java.util.Objects;

public class RangeWithBounds {
    public enum BoundType {
        INCLUDING,
        EXCLUDING
    }

    private Number min;
    private Number max;
    private BoundType leftBoundType;
    private BoundType rightBoundType;

    public RangeWithBounds(Number min, Number max) {
        this(min, max, BoundType.INCLUDING, BoundType.INCLUDING);
    }

    public RangeWithBounds(Number min, Number max, BoundType leftBoundType, BoundType rightBoundType) {
        this.min = min;
        this.max = max;
        this.leftBoundType = leftBoundType;
        this.rightBoundType = rightBoundType;
    }

    public BoundType getLeftBoundType() {
        return leftBoundType;
    }

    public void setLeftBoundType(BoundType leftBoundType) {
        this.leftBoundType = leftBoundType;
    }

    public BoundType getRightBoundType() {
        return rightBoundType;
    }

    public void setRightBoundType(BoundType rightBoundType) {
        this.rightBoundType = rightBoundType;
    }

    public Number getMax() {
        return max;
    }

    public Number getMin() {
        return min;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RangeWithBounds)) return false;
        RangeWithBounds that = (RangeWithBounds) o;
        return Objects.equals(min, that.min) &&
                Objects.equals(max, that.max) &&
                leftBoundType == that.leftBoundType &&
                rightBoundType == that.rightBoundType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, leftBoundType, rightBoundType);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (leftBoundType == BoundType.INCLUDING) {
            builder.append('[');
        } else {
            builder.append('(');
        }
        builder.append(min).append("..").append(max);
        if (rightBoundType == BoundType.INCLUDING) {
            builder.append(']');
        } else {
            builder.append(')');
        }
        return builder.toString();
    }

}
