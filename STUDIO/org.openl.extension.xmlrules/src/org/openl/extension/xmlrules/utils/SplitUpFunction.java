package org.openl.extension.xmlrules.utils;

import java.util.ArrayList;
import java.util.List;

public class SplitUpFunction {
    public static Object[][] splitUp(Object object) {
        if (object == null) {
            return new Object[][] {};
        }

        if (object.getClass().isArray()) {
            Object[] array = (Object[]) object;

            Class type = null;
            for (Object item : array) {
                if (item != null) {
                    type = item.getClass();
                }
            }
            if (type == null) {
                return new Object[][] {};
            }

            if (type.isArray()) {
                if (!(array instanceof Object[][])) {
                    Object[][] newArray = new Object[array.length][];
                    for (int i = 0; i < array.length; i++) {
                        Object[] row = (Object[]) array[i];
                        newArray[i] = row;
                    }
                    array = newArray;
                }
                return splitUp((Object[][]) array);
            } else {
                return splitUp(new Object[][] { array });
            }
        } else {
            return new Object[][] { { object } };
        }
    }

    public static Object[][] splitUp(Object[][] array) {
        List<Object> list = new ArrayList<Object>();

        for (Object[] row : array) {
            for (Object item : row) {
                if (item != null) {
                    list.add(item);
                }
            }
        }

        Object[][] result = new Object[list.size()][1];
        for (int i = 0; i < list.size(); i++) {
            result[i][0] = list.get(i);
        }

        return result;
    }
}
