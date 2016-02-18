package org.openl.extension.xmlrules.utils;

public class SumFunction {
    public static double sum(Object object) {
        if (object == null) {
            return 0;
        }

        if (object.getClass().isArray()) {
            return sum((Object[]) object);
        } else {
            return HelperFunctions.toDouble(object);
        }
    }


    public static double sum(Object[] array) {
        if (array == null) {
            return 0;
        }

        double sum = 0;

        for (Object o : array) {
            if (o != null && o.getClass().isArray()) {
                o = sum((Object[]) o);
            }
            Double value = HelperFunctions.toDouble(o);
            if (value != null) {
                sum += value;
            }
        }

        return sum;
    }
}
