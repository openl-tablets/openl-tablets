package org.openl.util;

public class RangeWithBounds {

    Number min;
    Number max;

    public RangeWithBounds(Number min, Number max) {
        super();
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RangeWithBounds other = (RangeWithBounds) obj;
        if (max == null) {
            if (other.max != null) {
                return false;
            }
        } else if (!max.equals(other.max)) {
            return false;
        }
        if (min == null) {
            if (other.min != null) {
                return false;
            }
        } else if (!min.equals(other.min)) {
            return false;
        }
        return true;
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
        return "[" + min + ".." + max + "]";
    }

}
