package org.openl.extension.xmlrules.utils;

public class IndexFunction {
    public static Object index(Object object, int userRow, int userColumn) {
        if (object == null) {
            return null;
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
                return null;
            }
            if (type.isArray()) {
                return index((Object[][]) array, userRow, userColumn);
            } else {
                return index(new Object[][] { array }, userRow, userColumn);
            }
        } else {
            return index(new Object[][] { { object } }, userRow, userColumn);
        }
    }

    public static Object index(Object[][] array, int userRow, int userColumn) {
        int row = userRow - 1;
        int column = userColumn - 1;

        if (array == null || array.length <= row) {
            return null;
        }

        if (array[row].length <= column) {
            return null;
        }

        if (userColumn == 0) {
            return array[row];
        }

        if (userRow == 0) {
            Object[][] result = new Object[array.length][1];
            for (int i = 0; i < array.length; i++) {
                result[i][0] = array[i][column];
            }
            return array[row][column];
        }

        return array[row][column];
    }
}
