package org.openl.ie.simplex;

/**
 * <p>
 * Title: <b>ArrayOut</b>
 * </p>
 * <p>
 * Description: The class being designed for outputing built-in arrays
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigengroup
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class ArrayOut {
    Object[] _arr = null;

    /**
     * Construtor from an array of doubles
     *
     * @param arr An array to be printed
     */
    public ArrayOut(double[] arr) {
        _arr = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            _arr[i] = new Double(arr[i]);
        }
    }

    /**
     * Constructor from an array of floats
     *
     * @param arr An array to be printed
     */
    public ArrayOut(float[] arr) {
        _arr = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            _arr[i] = new Float(arr[i]);
        }
    }

    /**
     * Constructor from an array of integers
     *
     * @param arr An array to be printed
     */
    public ArrayOut(int[] arr) {
        _arr = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            _arr[i] = new Integer(arr[i]);
        }
    }

    /**
     * Constructor of an array of {@link java.lang.Object}s
     *
     * @param arr An array to be printed
     */
    public ArrayOut(Object[] arr) {
        _arr = new Object[arr.length];
        System.arraycopy(arr, 0, _arr, 0, arr.length);
    }

    /**
     * @return String representation of an array.
     */
    @Override
    public String toString() {
        String str = "[";
        if (_arr.length > 0) {
            for (int i = 0; i < _arr.length - 1; i++) {
                str += _arr[i] + ", ";
            }
            str += _arr[_arr.length - 1];
        }
        str += "]";
        return str;
    }

}