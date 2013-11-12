package org.openl.ie.simplex;

/**
 * <p>
 * Title: LPConstraint
 * </p>
 * <p>
 * Description: The class being designed for facilitating the storing and
 * getting access to the information about LP constraint
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class LPConstraint extends VarType {
    private int[] _locations;
    private double[] _values;

    /**
     * Uses {@link VarType#LPX_FX} as the type for the constraint
     *
     * @param locs an array of locations of nonzero coefficients in the LP
     *            constraint
     * @param vals an array of values of non zero coefficients
     *            <p>
     *            Note: the i'th coefficient standing at the locs[i] position
     */
    public LPConstraint(int[] locs, double[] vals) {
        super();
        if (locs.length != vals.length) {
            throw new IllegalArgumentException("Array \"vals\" must be of the same length as \"locs\" ");
        }
        _locations = locs;
        _values = vals;
    }

    /**
     * Substitutes <code>Double.MAX_VALUE</code> for upper bound.
     *
     * @param locs an array of locations of nonzero coefficients in the LP
     *            constraint
     * @param vals an array of values of non zero coefficients
     *            <p>
     *            Note: the i'th coefficient standing at the locs[i] position
     * @param type The type of the constraint
     * @param lb the lower bound
     */
    public LPConstraint(int[] locs, double[] vals, int type, double lb) {
        this(locs, vals, type, lb, Double.MAX_VALUE);
    }

    /**
     * @param locs an array of locations of nonzero coefficients in the LP
     *            constraint
     * @param vals an array of values of non zero coefficients
     *            <p>
     *            Note: the i'th coefficient standing at the locs[i] position
     * @param type The type of the constraint
     * @param lb the lower bound
     * @param ub the upper bound
     * @see VarType
     */
    public LPConstraint(int[] locs, double[] vals, int type, double lb, double ub) {
        super(type, lb, ub);
        if (locs.length != vals.length) {
            throw new IllegalArgumentException("Array \"vals\" must be of the same length as \"locs\" ");
        }
        for (int i = 0; i < locs.length; i++) {
            if ((locs[i] < 0)) {
                throw new IllegalArgumentException("locs[" + i + "]=" + locs[i] + " : can't be negative");
            }
            if (vals[i] == 0) {
                throw new IllegalArgumentException("values[" + i + "]=0" + ": can't contain nill elements");
            }
        }
        _locations = locs;
        _values = vals;
    }

    /**
     * @return an array of locations of nonzero coefficients in the constraint
     *         concerned
     */
    public int[] getLocations() {
        return _locations;
    }

    /**
     * @return An array of nonzero coefficients
     *         <p>
     *         Note: the position of the particular value in the array
     *         corresponding to the ordinal number of the appropriate variable
     *         in the lp problem object
     */
    public double[] getValues() {
        return _values;
    }

    @Override
    public String toString() {
        String s = "[ ";
        s += super.toString();
        s += " MatRow: ";
        int size = _locations.length - 1;
        for (int i = 0; i < size; i++) {
            s += "" + _locations[i] + ":" + _values[i] + ", ";
        }
        s += _locations[size] + ":" + _values[size] + " ]";
        return s;
    }
}