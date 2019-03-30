package org.openl.rules.dt.algorithm.evaluator;

import java.util.Comparator;

import org.openl.rules.helpers.NumberUtils;

public class FloatTypeComparator implements Comparator<Object> {
    private static final FloatTypeComparator INSTANCE = new FloatTypeComparator();

    private FloatTypeComparator() {
    }

    public static FloatTypeComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(Object o1, Object o2) {
        double d1 = NumberUtils.convertToDouble(o1);
        double d2 = NumberUtils.convertToDouble(o2);
        int compare = Double.compare(d1, d2);
        if (compare == 0) {
            return 0;
        } else if (Math.abs(d1 - d2) <= Math.ulp(d1)) {
            return 0;
        } else {
            return compare;
        }
    }
}
