package org.openl.util;

import org.apache.commons.lang.builder.EqualsBuilder;

public class RangeWithBounds {
    public static enum BoundType {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RangeWithBounds)) {
            return false;
        }
        final RangeWithBounds other = (RangeWithBounds) obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(max, other.getMax());
        builder.append(min, other.getMin());
        builder.append(leftBoundType, other.getLeftBoundType());
        builder.append(rightBoundType, other.getRightBoundType());
        return builder.isEquals();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((max == null) ? 0 : max.hashCode());
        result = prime * result + ((min == null) ? 0 : min.hashCode());
        result = prime * result + leftBoundType.hashCode();
        result = prime * result + rightBoundType.hashCode();
        return result;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(leftBoundType == BoundType.INCLUDING){
            builder.append('[');
        } else{
            builder.append('(');
        }
        builder.append(min + ".." + max);
        if(rightBoundType == BoundType.INCLUDING){
            builder.append(']');
        } else{
            builder.append(')');
        }
        return builder.toString();
    }

}
