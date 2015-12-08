package org.openl.extension.xmlrules.utils;

public class InternalFunctions {
    public static String[][] Out(Object o, boolean horizontalRowValues, boolean showColumnNames) {
        return OutFunction.run(o, horizontalRowValues, showColumnNames);
    }

    public static String Out(int row, int column, Object o) {
        return Out(row, column, o, true, false);
    }

    public static String Out(int row, int column, Object o, boolean horizontalRowValues) {
        return Out(row, column, o, horizontalRowValues, false);
    }

    public static String Out(int row, int column, Object o, boolean horizontalRowValues, boolean showColumnNames) {
        String[][] out = Out(o, horizontalRowValues, showColumnNames);
        String result;
        if (out == null) {
            return "";
        }

        String[] columns;

        if (out.length <= row) {
            if (out.length != 1) {
                return "";
            }

            columns = out[0];
        }  else {
            columns = out[row];
        }

        if (columns.length <= column) {
            if (columns.length != 1) {
                return "";
            }

            result = columns[0];
        } else {
            result = columns[column];
        }

        return result == null ? "" : result;
    }
}
