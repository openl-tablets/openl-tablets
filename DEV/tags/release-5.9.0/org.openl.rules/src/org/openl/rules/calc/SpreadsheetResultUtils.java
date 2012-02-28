package org.openl.rules.calc;

import org.openl.rules.calc.result.SpreadsheetResultHelper;

/**
 * 
 * @author DLiauchuk
 * @deprecated 11.01.2011
 * Use {@link SpreadsheetResultHelper} instead
 */
@Deprecated
public class SpreadsheetResultUtils {
    private SpreadsheetResultUtils(){}
    
    public static void noMandatoryColumn(String mandatoryColumnName) {
        SpreadsheetResultHelper.noMandatoryColumn(mandatoryColumnName);
    }
    
    public static int getColumnIndex(String columnName, String[] colNames) {
        return SpreadsheetResultHelper.getColumnIndex(columnName, colNames);
    }
    
    public static int getColumnIndexByName(String columnName, String[] colNames) {
        return SpreadsheetResultHelper.getColumnIndexByName(columnName, colNames);
    }
}
