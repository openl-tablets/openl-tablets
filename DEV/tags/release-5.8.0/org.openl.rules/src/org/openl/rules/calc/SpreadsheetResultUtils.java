package org.openl.rules.calc;

import org.openl.exception.OpenlNotCheckedException;

public class SpreadsheetResultUtils {
    private SpreadsheetResultUtils(){}
    
    public static void noMandatoryColumn(String mandatoryColumnName) {
        String message = String.format("Spreadsheet does not have a mandatory column: %s", 
            mandatoryColumnName);
        throw new OpenlNotCheckedException(message);
    }


    public static int getColumnIndex(String columnName, String[] colNames) {
        for (int index = 0; index < colNames.length; index ++) {
            if (colNames[index].equals(columnName)
                    || (colNames[index].startsWith(columnName) && Character
                            .isSpaceChar(colNames[index].charAt(columnName.length()))))
                return index;
        }
        return -1;
    }
    
    public static int getColumnIndexByName(String columnName, String[] colNames) {
        int columnIndex = getColumnIndex(columnName, colNames);
        if (columnIndex < 0) {
            noMandatoryColumn(columnName);
        }
        return columnIndex;
    }
}
