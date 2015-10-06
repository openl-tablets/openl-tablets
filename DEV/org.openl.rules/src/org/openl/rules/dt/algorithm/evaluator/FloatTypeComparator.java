package org.openl.rules.dt.algorithm.evaluator;

import java.util.Comparator;

import org.openl.rules.helpers.NumberUtils;
import org.openl.util.math.MathUtils;

public class FloatTypeComparator implements Comparator<Object> {
    private static FloatTypeComparator INSTANCE = new FloatTypeComparator();
    
    private FloatTypeComparator() {
    }
    
    public static FloatTypeComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(Object o1, Object o2) {
        double d1 = NumberUtils.convertToDouble(o1);
        double d2 = NumberUtils.convertToDouble(o2);
        if (MathUtils.lt(d1, d2)) {
            return -1;
        }
        if (MathUtils.gt(d1, d2)) {
            return 1;
        }
        return 0;
    }
}