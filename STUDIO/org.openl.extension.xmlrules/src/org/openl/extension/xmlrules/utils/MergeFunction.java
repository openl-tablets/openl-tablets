package org.openl.extension.xmlrules.utils;

import java.util.ArrayList;
import java.util.List;

public class MergeFunction {
    public static Object[] merge(Object[][] array) {
        List<Object> result = new ArrayList<Object>();
        for (Object[] row : array) {
            for (Object item : row) {
                if (item != null) {
                    result.add(item);
                }
            }
        }

        return result.toArray(new Object[result.size()]);
    }
}
