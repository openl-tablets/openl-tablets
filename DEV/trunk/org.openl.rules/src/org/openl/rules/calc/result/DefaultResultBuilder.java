package org.openl.rules.calc.result;

import java.util.Map;

import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenField;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}. 
 *
 */
public class DefaultResultBuilder implements IResultBuilder {
    
    public Object makeResult(SpreadsheetResultCalculator result) {    
        
        int height = result.height();
        int width = result.width();
        
        Object resultArray[][] = new Object[height][width];
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                resultArray[row][col] = result.getValue(row, col);
            }
        }
        
        String[] rowNames = new String[height];        
        for (int row = 0; row < height; row++) {
            rowNames[row] = result.getRowName(row);
        }
         
        String[] columnNames = new String[width];        
        for (int col = 0; col < width; col++) {
            columnNames[col] = result.getColumnName(col);
        }
        
        TableSyntaxNode tsn = (TableSyntaxNode) result.getSpreadsheet().getInfo().getSyntaxNode();
        ILogicalTable table = tsn.getTableBody();
        
        Map<String, IOpenField> fields = result.getSpreadsheet().getSpreadsheetType().getFields();
        
        SpreadsheetResult spreadsheetBean = new SpreadsheetResult(resultArray, rowNames, columnNames, fields);
        spreadsheetBean.setLogicalTable(table);
        
        return spreadsheetBean;
    }
}
