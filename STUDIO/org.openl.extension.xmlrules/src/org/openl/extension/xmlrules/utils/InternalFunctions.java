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
        return out == null || out.length <= row || out[row].length <= column ?  "" : out[row][column];
    }
}
