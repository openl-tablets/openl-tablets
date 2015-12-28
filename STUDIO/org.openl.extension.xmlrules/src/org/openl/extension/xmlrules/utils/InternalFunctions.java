package org.openl.extension.xmlrules.utils;

public class InternalFunctions {
    public static String[][] Out(Object o) {
        return Out(o, true, false);
    }

    public static String[][] Out(Object o, boolean horizontalRowValues) {
        return Out(o, horizontalRowValues, false);
    }

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
        } else {
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

    public static Object[][] TRANSPOSE(Object[][] array) {
        return HelperFunctions.transpose(array);
    }

    public static Object[] ReadIn(String typeName, Object[][] array) {
        return ReadInFunction.readIn(typeName, array);
    }

    public static Object[][] SplitUp(Object object) {
        return SplitUpFunction.splitUp(object);
    }

    public static Object[][] SplitUp(Object[][] array) {
        return SplitUpFunction.splitUp(array);
    }

    public static Object SplitUp(int row, Object array) {
        Object[][] result = SplitUp(array);
        if (result == null || result.length <= row) {
            return "";
        }

        return result[row][0];
    }

    public static Object[] Merge(Object[][] array) {
        return MergeFunction.merge(array);
    }

    public static Object INDEX(Object object, int userRow, int userColumn) {
        return IndexFunction.index(object, userRow, userColumn);
    }

    public static double AVERAGE(Object[] array) {
        return AverageFunction.average(array);
    }

    public static int ROWS(Object array) {
        return ROWS((Object[][]) array);
    }

    public static int ROWS(Object[][] array) {
        return array.length;
    }

    public static int COLUMNS(Object array) {
        return COLUMNS((Object[][]) array);
    }

    public static int COLUMNS(Object[][] array) {
        return array[0].length;
    }

    public static double SUM(Object array) {
        return SumFunction.sum(array);
    }

    public static double SUM(Object[][] array) {
        return SumFunction.sum(array);
    }
}
