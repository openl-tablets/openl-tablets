package org.openl.ie.constrainer.lpsolver;

/**
 *
 * <p>Title: <b>FloatExpTable</b></p>
 * <p>Description: A very simple implementation of two dimensional array of <code>FloatExp</code>.
 *  It provides a basic set of routines allowing to get access to the elements of matrix by their
 *  row's and column's position to get access to the respective rows and columns, to multiply the
 *  matrix by an array of <code>FloatExp</code>, etc.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Tseitlin
 * @version 1.0
 */

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.ConstrainerObject;
import org.openl.ie.constrainer.ConstrainerObjectImpl;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;

public class FloatExpTable extends ConstrainerObjectImpl implements ConstrainerObject {
    protected FloatExpArray _array;
    protected int _nbRows;
    protected int _nbCols;

    public FloatExpTable(Constrainer c, int nbRows, int nbCols) {
        this(c, nbRows, nbCols, "");
    }

    public FloatExpTable(Constrainer c, int nbRows, int nbCols, String name) {
        super(c, name);
        _array = new FloatExpArray(c, nbRows * nbCols);
        _nbRows = nbRows;
        _nbCols = nbCols;
    }

    /**
     * Returns table's element with coordinates (m,n)
     *
     * @param m Row number
     * @param n Column number
     * @return <code>FloatExp</code>
     */
    public FloatExp elementAt(int m, int n) {
        return _array.elementAt(m * _nbCols + n);
    }

    /**
     * Returns table's element with coordinates (m,n)
     *
     * @param m Row number
     * @param n Column number
     * @return <code>FloatExp</code>
     */
    public FloatExp get(int m, int n) {
        return _array.get(m * _nbCols + n);
    }

    /**
     * @param n Column's number
     * @return <code>FloatExpArray</code> corresponds to the n'th column of
     *         the table
     */
    public FloatExpArray getColumn(int n) {
        if (n >= _nbCols) {
            throw new java.lang.IndexOutOfBoundsException();
        }
        FloatExpArray arr = new FloatExpArray(_array.constrainer(), _nbRows);
        FloatExp[] source = _array.data();
        FloatExp[] dest = arr.data();
        int start = n;
        int end = _nbCols * (_nbRows - 1) + n;
        int step = _nbCols;
        ;
        ;
        int counter = 0;
        for (int i = start; i <= end; i += step) {
            dest[counter] = source[i];
            counter++;
        }
        return arr;
    }

    /**
     * @param m Row number
     * @return <code>FloatExpArray</code> corresponds to the m'th row of the
     *         table
     */
    public FloatExpArray getRow(int m) {
        if (m >= _nbRows) {
            throw new java.lang.IndexOutOfBoundsException();
        }
        FloatExpArray arr = new FloatExpArray(_array.constrainer(), _nbCols);
        FloatExp[] source = _array.data();
        FloatExp[] dest = arr.data();
        int start = m * _nbCols;
        int end = m * _nbCols + _nbCols;
        System.arraycopy(source, start, dest, 0, end - start);
        return arr;
    }

    /**
     * Returns the maximal value for all expressions in this array.
     *
     * @return Maximal value for all epressions in the array
     */
    public double max() {
        return _array.max();
    }

    /**
     * Returns the minimal value for all expressions in this array.
     *
     * @return Minimal value for all epressions in the array
     */
    public double min() {
        return _array.min();
    }

    /**
     * Returns the expression for the product of the two dimensional array and
     * array of double
     *
     * @param vec Array of double
     * @return <code>FloatExpArray</code> the i'th element of which equals to
     *         the scalar product of the i'th row and input array.
     */
    public FloatExpArray mul(double[] vec) {
        if (_nbCols != vec.length) {
            throw new IllegalArgumentException("Incorrect vector length (must be equal to the number of columns)");
        }
        FloatExpArray arr = new FloatExpArray(constrainer(), _nbRows);
        for (int i = 0; i < arr.size(); i++) {
            arr.set(constrainer().scalarProduct(getRow(i), vec), i);
        }
        return arr;
    }

    /**
     * Assignes some <code>FloatExp</code> to the element of the table with
     * coordinates (m,n)
     *
     * @param exp The expression to be associated with the table's element
     * @param m Row's number
     * @param n Column's number
     */
    public void set(FloatExp exp, int m, int n) {
        _array.set(exp, m * _nbCols + n);
    }

    /**
     * Returns an expression for the sum of all elements in the array
     *
     * @return Expression for the sum of all elements in the array
     */
    public FloatExp sum() {
        return _array.sum();
    }

    /**
     * Returns an array representation of the table
     *
     * @return <code>FloatExpArray</code> the i'th element of which equals to
     *         the (i/nbCols, i%nbCols) element of the table.
     */
    public FloatExpArray toFloatExpArray() {
        return _array;
    }
}