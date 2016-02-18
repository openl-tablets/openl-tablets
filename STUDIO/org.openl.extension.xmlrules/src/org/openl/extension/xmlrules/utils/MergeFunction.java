package org.openl.extension.xmlrules.utils;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.ArrayTool;

public class MergeFunction {
    public static Object[] merge(Object[][] array) {
        List<Object> result = new ArrayList<Object>();
        for (Object[] row : array) {
            mergeRow(result, row);
        }

        return result.toArray(new Object[result.size()]);
    }

    private static void mergeRow(List<Object> result, Object[] row) {
        for (Object item : row) {
            if (item != null) {
                if (item.getClass().isArray()) {
                    if (item instanceof Object[]) {
                        mergeRow(result, (Object[]) item);
                    } else {
                        mergeRow(result, ArrayTool.toArray(item));
                    }
                } else {
                    result.add(item);
                }
            }
        }
    }
}
