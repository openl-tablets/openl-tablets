/*
 * Created on Apr 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util;

/**
 * @author snshor
 */
public class MultiIndexIterator {

    int[] dims;
    int[] values;
    boolean firstTime = true;

    public MultiIndexIterator(int[] dims) {
        this.dims = dims;
        values = new int[dims.length];

        firstTime = dims.length > 0;

        for (int i = 0; i < dims.length; i++) {
            if (dims[i] == 0) {
                firstTime = false;
            }
        }

    }

    public boolean hasNext() {
        if (firstTime) {
            return true;
        }
        for (int i = 0; i < dims.length; ++i) {
            if (values[i] < dims[i] - 1) {
                return true;
            }
        }
        return false;
    }

    public int[] nextDim() {
        if (firstTime) {
            firstTime = false;
            return values;
        }

        for (int i = 0; i < values.length; i++) {
            if (++values[i] < dims[i]) {
                break;
            }

            values[i] = 0;

        }

        return values;
    }

}
