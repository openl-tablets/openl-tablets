package org.openl.ie.constrainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.openl.ie.constrainer.impl.IntExpAddArray1;
import org.openl.ie.constrainer.impl.IntExpArrayElement1;
import org.openl.ie.tools.FastVector;

/**
 * An implementation of the array of the conatraint integer expressions.
 */
public final class IntExpArray extends ConstrainerObjectImpl {

    private IntExp[] _data;

    private IntArrayCards _cards = null;

    /**
     * Constructor from the FastVector.
     */
    public IntExpArray(Constrainer c, FastVector v) {
        this(c, v.size());

        for (int i = 0; i < _data.length; ++i) {
            _data[i] = (IntExp) v.elementAt(i);
        }
    }

    /**
     * Constructor for "size" unitialized expressions. Call set() to initialize all the expressions in this array.
     */
    public IntExpArray(Constrainer c, int size) {
        super(c);
        _data = new IntExp[size];
    }

    public IntExpArray(Constrainer c, int size, int min, int max, String array_name) {
        this(c, size);

        name(array_name);

        for (int i = 0; i < _data.length; ++i) {
            _data[i] = c.addIntVar(min, max, array_name + "(" + i + ")");
        }
    }

    /**
     * Creates an array of "size" constrained integer variables defined from "min" to "max".
     *
     * @param size size of the new array
     * @param min minimal value of each constrained variable
     * @param max maximal value of each constrained variable
     */
    public IntExpArray(Constrainer c, int size, int min, int max, String array_name, int int_domain) {
        this(c, size);

        name(array_name);

        for (int i = 0; i < _data.length; ++i) {
            _data[i] = c.addIntVar(min, max, array_name + "(" + i + ")", int_domain);
        }
    }

    /**
     * Convenience constructor from one expression.
     */
    public IntExpArray(Constrainer c, IntExp e0) {
        this(c, 1);

        _data[0] = e0;
    }

    /**
     * Convenience constructor from 2 expressions.
     */
    public IntExpArray(Constrainer c, IntExp e0, IntExp e1) {
        this(c, 2);

        _data[0] = e0;
        _data[1] = e1;

    }

    /**
     * Convenience constructor from 3 expressions.
     */
    public IntExpArray(Constrainer c, IntExp e0, IntExp e1, IntExp e2) {
        this(c, 3);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
    }

    /**
     * Convenience constructor from 4 expressions.
     */
    public IntExpArray(Constrainer c, IntExp e0, IntExp e1, IntExp e2, IntExp e3) {
        this(c, 4);

        _data[0] = e0;
        _data[1] = e1;
        _data[2] = e2;
        _data[3] = e3;
    }

    /**
     * Convenience constructor from 5 expressions.
     */
    public IntExpArray(Constrainer c, IntExp e0, IntExp e1, IntExp e2, IntExp e3, IntExp e4) {
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
    public IntExpArray(Constrainer c, IntExp e0, IntExp e1, IntExp e2, IntExp e3, IntExp e4, IntExp e5) {
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
    public IntExpArray(Constrainer c, IntExp e0, IntExp e1, IntExp e2, IntExp e3, IntExp e4, IntExp e5, IntExp e6) {
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
    public IntExpArray(Constrainer c,
            IntExp e0,
            IntExp e1,
            IntExp e2,
            IntExp e3,
            IntExp e4,
            IntExp e5,
            IntExp e6,
            IntExp e7) {
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
    public IntExpArray(Constrainer c,
            IntExp e0,
            IntExp e1,
            IntExp e2,
            IntExp e3,
            IntExp e4,
            IntExp e5,
            IntExp e6,
            IntExp e7,
            IntExp e8) {
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
     * Constructor for "size" unitialized expressions. Call set() to initialize all the expressions in this array.
     */
    public IntExpArray(Constrainer c, IntExp[] vars) {
        super(c);
        _data = new IntExp[vars.length];
        System.arraycopy(vars, 0, _data, 0, vars.length);
    }

    /**
     * Constructor from the Vector.
     */
    public IntExpArray(Constrainer c, List<IntExp> v) {
        this(c, v.size());

        for (int i = 0; i < _data.length; ++i) {
            _data[i] = v.get(i);
        }
    }

    /**
     * Returns an array of cardinalities for the expressions. If such an array doesn't exist it will be created using
     * invokation <code>IntArrayCards(this.constrainer(),this)</code>
     *
     * @see IntArrayCards
     */
    public IntArrayCards cards() throws Failure {
        if (_cards == null) {
            _cards = new IntArrayCards(_constrainer, this);
        }
        return _cards;
    }

    /**
     * Returns the internal array of the expressions.
     */
    public IntExp[] data() {
        return _data;
    }

    /**
     * Returns the i-th element of this array.
     */
    public IntExp elementAt(int idx) {
        return _data[idx];
    }

    /**
     * Returns the expression that corresponds to the array item where indes is specified by the parameter expressions.
     *
     * @param exp expressions that specifies array element index. <b>NOTE:</b> The expression is not required to be
     *            bound.
     *
     * @return <code>this[exp]</code>
     */
    public IntExp elementAt(IntExp exp) throws Failure {
        return new IntExpArrayElement1(this, exp);
    }
    /* EO additions */

    /**
     * Returns the i-th element of this array.
     */
    public IntExp get(int idx) {
        return _data[idx];
    }

    /**
     * Returns the maximal value for all expressions in this array.
     */
    public int max() {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < _data.length; ++i) {
            int maxi = _data[i].max();
            if (maxi > max) {
                max = maxi;
            }
        }
        return max;
    }

    /**
     * The function merges this array and the array provided as parameter and returns the resulted array.
     *
     * @param array the array to be merged
     *
     * @return the merged array
     */
    public IntExpArray merge(IntExpArray array) {
        int i;
        List<IntExp> new_data = new ArrayList<>(_data.length + array._data.length);

        for (i = 0; i < _data.length; i++) {
            new_data.add(_data[i]);
        }

        for (i = 0; i < array._data.length; i++) {
            new_data.add(array._data[i]);
        }

        return new IntExpArray(constrainer(), new_data);
    }

    /**
     * Returns the minimal value for all expressions in this array.
     */
    public int min() {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < _data.length; ++i) {
            int mini = _data[i].min();
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
     * Sets up the i-th element of this array.
     */
    public void set(IntExp exp, int idx) {
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
    public void sort(Comparator<IntExp> c) {
        Arrays.sort(_data, c);
    }

    /**
     * Sorts the internal array of the expressions by number of dependents.
     */
    public void sortByDependents() // not optimized
    {
        sort(new Comparator<IntExp>() {
            @Override
            public int compare(IntExp o1, IntExp o2) {
                return o2.allDependents().size() - o1.allDependents().size();
            }
        });
    }

    /*
     * extending to 5.1.0 added by S. Vanskov
     */
    /**
     * The function returns subarray of the array consisting of the given array elements which indeses more or equal to
     * <code> min_index </code> and more or equal to <code> max_index </code>
     *
     * @param min_index index lower bound
     * @param max_index index upper bound
     *
     * @return subarray of array from <code> min_index </code> to <code> max_index </code>
     */
    public IntExpArray subarray(int min_index, int max_index) {
        if (min_index > max_index) {
            return new IntExpArray(constrainer(), 0);
        }

        List<IntExp> sub_data = new ArrayList<>(max_index - min_index + 1);

        for (int i = min_index; i <= max_index; i++) {
            sub_data.add(_data[i]);
        }

        return new IntExpArray(constrainer(), sub_data);
    }

    /**
     * The function returns subarray of the array according to the mask array.
     *
     * @param mask is the mask array
     *
     * @return the subarray according the mask array
     */
    public IntExpArray subarrayByMask(boolean[] mask) {
        int size = Math.min(_data.length, mask.length);

        List<IntExp> sub_data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (mask[i]) {
                sub_data.add(_data[i]);
            }
        }

        return new IntExpArray(constrainer(), sub_data);
    }

    /**
     * Returns an expression for the sum of all expressions in this array.
     */
    public IntExp sum() {
        switch (size()) {
            case 0:
                // return new IntExpConst(constrainer(),0);
                return (IntExp) _constrainer.expressionFactory()
                    .getExpression(IntExpConst.class, new Object[] { _constrainer, new Integer(0) });

            case 1:
                return _data[0];

            case 2:
                return _data[0].add(_data[1]);

            default:
                // return new IntExpAddArray(_constrainer, this);
                return (IntExp) _constrainer.expressionFactory()
                    .getExpression(
                        // IntExpAddArray.class,
                        IntExpAddArray1.class,
                        new Object[] { _constrainer, this });

        }
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
} // ~IntExpArray
