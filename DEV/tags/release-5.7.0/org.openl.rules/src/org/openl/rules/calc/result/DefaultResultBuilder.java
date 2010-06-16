package org.openl.rules.calc.result;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IOpenField;

/**
 * Builder is used when return type of the spreadsheet table is {@link SpreadsheetResult}. 
 *
 */
public class DefaultResultBuilder implements IResultBuilder {
    
    public Object makeResult(SpreadsheetResultCalculator result) {    
        
        Object resultArray[][] = getResultArray(result);
        
        String[] rowNames = getRowNames(result);
         
        String[] columnNames = getColumnNames(result);
        
        Map<String, Point> fieldsCoordinates = getFieldsCoordinates(result);
        
        SpreadsheetResult spreadsheetBean = new SpreadsheetResult(resultArray, rowNames, columnNames, fieldsCoordinates);
        
        ILogicalTable table = getSpreadsheetTable(result);        
        spreadsheetBean.setLogicalTable(table);
        
        return spreadsheetBean;
    }

    private ILogicalTable getSpreadsheetTable(SpreadsheetResultCalculator result) {
        TableSyntaxNode tsn = (TableSyntaxNode) result.getSpreadsheet().getInfo().getSyntaxNode();
        
        ILogicalTable table = null;
        if (tsn != null) {
            table = tsn.getTableBody();
        }        
        return table;
    }

    private String[] getColumnNames(SpreadsheetResultCalculator result) {
        int width = result.width();
        
        String[] columnNames = new String[width];        
        for (int col = 0; col < width; col++) {
            columnNames[col] = result.getColumnName(col);
        }
        return columnNames;
    }

    private String[] getRowNames(SpreadsheetResultCalculator result) {
        int height = result.height();
        
        String[] rowNames = new String[height];        
        for (int row = 0; row < height; row++) {
            rowNames[row] = result.getRowName(row);
        }
        return rowNames;
    }
    
    private Map<String, Point> getFieldsCoordinates(SpreadsheetResultCalculator result) {
        Map<String, IOpenField> fields = result.getSpreadsheet().getSpreadsheetType().getFields();
        
        Map<String, Point> fieldsCoordinates = new HashMap<String, Point>();
        for (Map.Entry<String, IOpenField> fieldEntry : fields.entrySet()) {     
            if (fieldEntry.getValue() instanceof SpreadsheetCellField) {
                SpreadsheetCellField cellField = (SpreadsheetCellField) fieldEntry.getValue();
                int row = cellField.getCell().getRowIndex();
                int column = cellField.getCell().getColumnIndex();
                
                fieldsCoordinates.put(fieldEntry.getKey(), new Point(column, row));
            }
        }
        return fieldsCoordinates;
    }
    
    private Object[][] getResultArray(SpreadsheetResultCalculator result) {
        int height = result.height();
        int width = result.width();
        
        Object resultArray[][] = new Object[height][width];
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                resultArray[row][col] = result.getValue(row, col);
            }
        }
        return resultArray;
    }
}
