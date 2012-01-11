package org.openl.rules.calc.result;

import org.apache.commons.lang.ClassUtils;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.SpreadsheetResult;

public class SpreadsheetResultHelper {
    private SpreadsheetResultHelper(){}
    
    public static boolean isSpreadsheetResult(Class<?> clazz) {
        return ClassUtils.isAssignable(clazz, SpreadsheetResult.class, false);
    }
    
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
