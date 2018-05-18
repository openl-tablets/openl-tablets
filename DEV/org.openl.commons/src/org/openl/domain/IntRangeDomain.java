/*
 * Created on Apr 28, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.Iterator;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author snshor
 */
public class IntRangeDomain extends FixedSizeDomain<Integer> implements IIntDomain {
    private class RangeIterator extends AIntIterator {
        private int current, step;

        RangeIterator(int step) {
            this.current = min;
            this.step = step;
        }

        public boolean hasNext() {
            return current <= max;
        }

        public int nextInt() {
        	int ret = current;
        	current += step;
            return ret;
        }

        @Override
        public int size() {
            return (max - min + 1) / step;
        }

		@Override
		public boolean isResetable() {
			return true;
		}

		@Override
		public void reset() {
			current = min;
		}
    }

    protected int min, max;

    public IntRangeDomain(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public boolean contains(int value) {
        return min <= value && value <= max;
    }

    public boolean containsNumber(Number n) {
        return min <= n.doubleValue() && n.doubleValue() <= max;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IntRangeDomain)) {
            return false;
        }

        IntRangeDomain other = (IntRangeDomain) obj;

        return min == other.min && max == other.max;
    }

    @XmlTransient
    public IType getElementType() {
        return null;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;

        hashCode = 31 * hashCode + min;
        hashCode = 31 * hashCode + max;

        return hashCode;
    }

    public IIntIterator intIterator() {
        return new RangeIterator(1);
    }
    
    public IIntIterator iterate(int step) {
        return new RangeIterator(step);
    }
    

    public Iterator<Integer> iterator() {
        return intIterator();
    }

    public boolean selectObject(Integer n) {
        return containsNumber(n);
    }

    public boolean selectType(IType type) {
        // FIXME
        return true;
    }

    public int size() {
        return max - min + 1;
    }

    @Override
    public String toString() {
        if (min == Integer.MIN_VALUE) {
            return "<=" + max;
        } else if (max == Integer.MAX_VALUE) {
            return ">=" + min;
        }

        return "[" + min + ".." + max + "]";
    }

}
