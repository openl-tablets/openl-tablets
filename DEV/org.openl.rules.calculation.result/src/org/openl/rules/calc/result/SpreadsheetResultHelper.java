package org.openl.rules.calc.result;

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
            String trimmedColName = colNames[index].trim();
            if (trimmedColName.equals(columnName)
                    || (trimmedColName.startsWith(columnName) && (Character.isSpaceChar(trimmedColName
                            .charAt(columnName.length())) || Character.valueOf(':').equals(
                            trimmedColName.charAt(columnName.length())))))
                return index;
        }
        return -1;
    }

    public static int getColumnIndexByName(String columnName, String[] colNames) {
        if (columnName == null) {
            throw new IllegalArgumentException("columnName arg can't be null");
        }
        if (colNames == null) {
            throw new IllegalArgumentException("colNames arg can't be null");
        }
        if (!columnName.trim().equals(columnName)) {
            throw new IllegalArgumentException("Invalid columnName format");
        }

        int columnIndex = getColumnIndex(columnName, colNames);
        if (columnIndex < 0) {
            throw new IndexOutOfBoundsException("Spreadsheet does not have a mandatory column: " + columnName);
        }
        return columnIndex;
    }
}
