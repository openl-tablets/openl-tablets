package org.openl.extension.xmlrules.utils;

import org.openl.util.ArrayTool;

public class AverageFunction {
    public static double average(Object[] array) {
        if (array == null) {
            return 0;
        }
        double sum = 0;

        for (Object o : array) {
            if (o != null) {
                if (o instanceof Number) {
                    sum += ((Number) o).doubleValue();
                } else if (o instanceof String) {
                    sum += Double.valueOf((String) o);
                } else if (o instanceof Object[]) {
                    sum += average((Object[]) o);
                } else if (o.getClass().isArray()) {
                    sum += average(ArrayTool.toArray(o));
                } else {
                    throw new IllegalArgumentException("Unsupported type '" + o.getClass().getCanonicalName() + "'");
                }
            }
        }

        return sum / array.length;
    }
}