package org.openl.ie.constrainer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import org.openl.ie.constrainer.impl.FloatExpAddArray;
import org.openl.ie.tools.FastVector;


/**
 * An implementation of the array of the conatraint floating-point expressions.
 */
public final class FloatExpArray extends ConstrainerObjectImpl {

    private FloatExp[] _data;

    /**
     * Constructor from the FastVector.
     */
    public FloatExpArray(Constrainer c, FastVector v) {
        this(c, v.size());

        for (int i = 0; i < _data.length; ++i) {
            _data[i] = (FloatExp) v.elementAt(i);
        }
    }

    /**
     * Convenience constructor from one expression.
     */
    public FloatExpArray(Constrainer c, FloatExp e0) {
        this(c, 1);

        _data[0] = e0;
    }

    /**
     * Convenience constructor from 2 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1) {
        this(c, 2);

        _data[0] = e0;
        _data[1] = e1;
    }

    /**
     * Convenience constructor from 3 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1, FloatExp e2) {
        this(c, 3);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
    }

    /**
     * Convenience constructor from 4 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1, FloatExp e2, FloatExp e3) {
        this(c, 4);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
        _data[3] = e3;
    }

    /**
     * Convenience constructor from 5 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1, FloatExp e2, FloatExp e3, FloatExp e4) {
        this(c, 5);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
        _data[3] = e3;
        _data[4] = e4;
    }

    /**
     * Convenience constructor from 6 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1, FloatExp e2, FloatExp e3, FloatExp e4, FloatExp e5) {
        this(c, 6);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
        _data[3] = e3;
        _data[4] = e4;
        _data[5] = e5;
    }

    /**
     * Convenience constructor from 7 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1, FloatExp e2, FloatExp e3, FloatExp e4, FloatExp e5,
            FloatExp e6) {
        this(c, 7);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
        _data[3] = e3;
        _data[4] = e4;
        _data[5] = e5;
        _data[6] = e6;
    }

    /**
     * Convenience constructor from 8 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1, FloatExp e2, FloatExp e3, FloatExp e4, FloatExp e5,
            FloatExp e6, FloatExp e7) {
        this(c, 8);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
        _data[3] = e3;
        _data[4] = e4;
        _data[5] = e5;
        _data[6] = e6;
        _data[7] = e7;
    }

    /**
     * Convenience constructor from 9 expressions.
     */
    public FloatExpArray(Constrainer c, FloatExp e0, FloatExp e1, FloatExp e2, FloatExp e3, FloatExp e4, FloatExp e5,
            FloatExp e6, FloatExp e7, FloatExp e8) {
        this(c, 9);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
        _data[3] = e3;
        _data[4] = e4;
        _data[5] = e5;
        _data[6] = e6;
        _data[7] = e7;
        _data[8] = e8;
    }

    /**
     * Constructor for "size" unitialized expressions. Call set() to initialize
     * all the expressions in this array.
     */
    public FloatExpArray(Constrainer c, int size) {
        super(c);
        _data = new FloatExp[size];
    }

    /**
     * Constructor from the Vector.
     */
    public FloatExpArray(Constrainer c, Vector v) {
        this(c, v.size());

        for (int i = 0; i < _data.length; ++i) {
            _data[i] = (FloatExp) v.elementAt(i);
        }
    }

    /**
     * Constructor for an sub-array.
     */
    FloatExpArray(FloatExpArray ary, int indexStart, int size) {
        this(ary.constrainer(), size);
        System.arraycopy(ary._data, indexStart, _data, 0, _data.length);

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

    /**
     * Returns an expression for the sum of all expressions in this array.
     */
    public FloatExp sum() {
        return sumAsTree();
        // return sumAsAddArray();
    }

    /**
     * Sum as FloatExpAddArray.
     */
    FloatExp sumAsAddArray() {
        switch (size()) {
            case 0:
                // return new FloatExpConst(constrainer(),0);
                return (FloatExp) _constrainer.expressionFactory().getExpression(FloatExpConst.class,
                        new Object[] { _constrainer, new Double(0) });

            case 1:
                return _data[0];

            case 2:
                return _data[0].add(_data[1]);

            default:
                // return new FloatExpAddArray(_constrainer, this);
                return (FloatExp) _constrainer.expressionFactory().getExpression(FloatExpAddArray.class,
                        new Object[] { _constrainer, this });

        }
    }

    /**
     * Sum as a tree of sub-sums.
     */
    FloatExp sumAsTree() {
        final int maxNodeArity = 10;

        int size = size();

        if (size <= maxNodeArity) {
            return sumAsAddArray();
        }

        int nSubSums, subSumSize;

        subSumSize = maxNodeArity;
        nSubSums = size / subSumSize;

        int reminder = size % subSumSize;
        if (reminder > 0) {
            nSubSums++;
        }

        FloatExpArray subSums = new FloatExpArray(constrainer(), nSubSums);
        for (int i = 0; i < nSubSums; ++i) {
            int indexStart = i * subSumSize;
            int sz = Math.min(subSumSize, size - indexStart);
            FloatExpArray subSum = new FloatExpArray(this, indexStart, sz);
            subSums.set(subSum.sumAsAddArray(), i);
        }

        return subSums.sumAsTree();
    }

    /**
     * Returns a String representation of this array.
     *
     * @return a String representation of this array.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("[");

        for (int i = 0; i < _data.length; ++i) {
            if (i > 0) {
                buf.append(" ");
            }
            buf.append(_data[i]);
        }

        buf.append("]");

        return buf.toString();
    }

} // ~FloatExpArray
