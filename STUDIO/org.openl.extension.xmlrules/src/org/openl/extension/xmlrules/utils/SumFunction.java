package org.openl.extension.xmlrules.utils;

public class SumFunction {
    public static double sum(Object object) {
        if (object == null) {
            return 0;
        }

        if (object.getClass().isArray()) {
            Object[] array = (Object[]) object;

            for (Object o : array) {
                if (o != null) {
                    if (o.getClass().isArray()) {
                        return sum((Object[][]) array);
                    } else {
                        return sum(array);
                    }
                }
            }

            return 0;
        } else {
            return HelperFunctions.toDouble(object);
        }
    }


    public static double sum(Object[] array) {
        double sum = 0;

        for (Object o : array) {
            Double value = HelperFunctions.toDouble(o);
            if (value != null) {
                sum += value;
            }
        }

        return sum;
    }

    public static double sum(Object[][] array) {
        double sum = 0;

        for (Object[] row : array) {
            sum += sum(row);
        }

        return sum;
    }
}
