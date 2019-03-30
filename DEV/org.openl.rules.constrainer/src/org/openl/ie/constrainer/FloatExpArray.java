package org.openl.ie.constrainer;

import java.util.Arrays;
import java.util.Comparator;

/**
 * An implementation of the array of the conatraint floating-point expressions.
 */
public final class FloatExpArray extends ConstrainerObjectImpl {

    private FloatExp[] _data;

    /**
     * Constructor for "size" unitialized expressions. Call set() to initialize all the expressions in this array.
     */
    public FloatExpArray(Constrainer c, int size) {
        super(c);
        _data = new FloatExp[size];
    }

    /**
     * Returns the internal array of the expressions.
     */
    public FloatExp[] data() {
        return _data;
    }

    /**
     * Returns i-th element of this array.
     */
    public FloatExp elementAt(int idx) {
        return _data[idx];
    }

    /**
     * Returns i-th element of this array.
     */
    public FloatExp get(int idx) {
        return _data[idx];
    }

    /**
     * Returns the maximal value for all expressions in this array.
     */
    public double max() {
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < _data.length; ++i) {
            double maxi = _data[i].max();
            if (maxi > max) {
                max = maxi;
            }
        }
        return max;
    }

    /**
     * Returns the minimal value for all expressions in this array.
     */
    public double min() {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < _data.length; ++i) {
            double mini = _data[i].min();
            if (mini < min) {
                min = mini;
            }
        }
        return min;
    }

    @Override
    public void name(String name) {
        symbolicName(name);
    }

    /**
     * Sets i-th element of this array.
     */
    public void set(FloatExp exp, int idx) {
        _data[idx] = exp;
    }

    /**
     * Returns the number of elements in this array.
     */
    public int size() {
        return _data.length;
    }

    /**
     * Sorts the internal array of the expressions using given comparator.
     */
    public void sort(Comparator c) {
        Arrays.sort(_data, c);
    }
} // ~FloatExpArray
