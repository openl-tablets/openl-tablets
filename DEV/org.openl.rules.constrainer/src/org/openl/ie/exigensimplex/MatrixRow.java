package org.openl.ie.exigensimplex;

/**
 * <p>
 * Title: MatrixRow
 * </p>
 * <p>
 * Description: The class is designed to be a storage for any sparse array and
 * is used to keep data of rows and columns of a constraint matrix
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
public class MatrixRow {
    private int[] _locations;
    private double[] _values;

    /**
     * Constructs a matrixrow based on array of doubles. NOTE: It parses an
     * input and creates an array of non zero values, alon with array of their
     * indices
     *
     * @param values Array of double
     */
    public MatrixRow(double[] values) {
        int nz = 0;
        int[] locations = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != 0) {
                locations[nz] = i;
                nz++;
            }
        }
        if (nz == 0) {
            throw new IllegalArgumentException("Nill array");
        }

        int locs[] = new int[nz];
        double vals[] = new double[nz];
        for (int i = 0; i < nz; i++) {
            locs[i] = locations[i];
            vals[i] = values[locations[i]];
        }
        _locations = locs;
        _values = vals;
    }

    /**
     * Constructs a matrixrow based on two arrays: array of indices and array of
     * values. NOTE: It doesn't perform a checking of it's input arguments. So
     * it is highly not recommended to use this constructor.
     *
     * @param locations An array of indices of non zero values
     * @param values An array of values
     */
    public MatrixRow(int[] locations, double[] values) {
        _locations = locations;
        _values = values;
    }

    /**
     * @return An array of indices
     */
    public int[] getLocations() {
        return _locations;
    }

    /**
     * @return An array of values
     */
    public double[] getValues() {
        return _values;
    }

    @Override
    public String toString() {
        String str = "{";
        for (int i = 0; i < _values.length; i++) {
            str += "(" + _locations[i] + "," + _values[i] + ")";
        }
        return (str + "}");
    }

}