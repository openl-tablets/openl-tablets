package org.openl.rules.calc.result;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


public class SpreadsheetResultHelper {
    private SpreadsheetResultHelper() {
    }

    public static int getColumnIndex(String columnName, String[] colNames) {
        if (columnName == null) {
            throw new IllegalArgumentException("columnName arg can't be null");
        }
        if (colNames == null) {
            throw new IllegalArgumentException("colNames arg can't be null");
        }
        if (!columnName.trim().equals(columnName)) {
            throw new IllegalArgumentException("Invalid columnName format");
        }
        for (int index = 0; index < colNames.length; index++) {
            if (colNames[index] != null){
                String trimmedColName = colNames[index].trim();
                if (trimmedColName.equals(columnName)
                        || (trimmedColName.startsWith(columnName) && (Character.isSpaceChar(trimmedColName
                                .charAt(columnName.length())) || Character.valueOf(':').equals(
                                trimmedColName.charAt(columnName.length())))))
                    return index;
            }
        }
        return -1;
    }

    public static int getColumnIndexByName(String columnName, String[] colNames) {
        int columnIndex = getColumnIndex(columnName, colNames);
        if (columnIndex < 0) {
            throw new IndexOutOfBoundsException("Spreadsheet does not have a mandatory column: " + columnName);
        }
        return columnIndex;
    }
}
