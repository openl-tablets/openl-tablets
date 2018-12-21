package org.openl.rules.calc.result;

import java.util.Map;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}. 
 *
 */
public class DefaultResultBuilder implements IResultBuilder {
    
    public Object makeResult(SpreadsheetResultCalculator result) {    
        
        Object resultArray[][] = result.getValues();

        Spreadsheet spreadsheet = result.getSpreadsheet();

        String[] rowNames = spreadsheet.getRowNames();
        String[] columnNames = spreadsheet.getColumnNames();
        String[] rowTitles = spreadsheet.getRowTitles();
        String[] columnTitles = spreadsheet.getColumnTitles();
        Map<String, Point> fieldsCoordinates = spreadsheet.getFieldsCoordinates();

        SpreadsheetResult spreadsheetBean = new SpreadsheetResult(resultArray, rowNames, columnNames, rowTitles, columnTitles, fieldsCoordinates);

        ILogicalTable table = getSpreadsheetTable(result);        
        spreadsheetBean.setLogicalTable(table);
        
        return spreadsheetBean;
    }

    private ILogicalTable getSpreadsheetTable(SpreadsheetResultCalculator result) {
        TableSyntaxNode tsn = result.getSpreadsheet().getSyntaxNode();
        
        ILogicalTable table = null;
        if (tsn != null) {
            table = tsn.getTableBody();
        }        
        return table;
    }
}
