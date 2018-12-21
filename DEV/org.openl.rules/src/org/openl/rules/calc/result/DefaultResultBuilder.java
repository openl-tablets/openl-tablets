package org.openl.rules.calc.result;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;
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

    public static Map<String, Point> getFieldsCoordinates(Map<String, IOpenField> spreadsheetfields) {
        Map<String, Point> fieldsCoordinates = new HashMap<String, Point>();
        for (Map.Entry<String, IOpenField> fieldEntry : spreadsheetfields.entrySet()) {
            Point fieldCoordinates = getRelativeSpreadsheetFieldCoordinates(fieldEntry.getValue());
            if (fieldCoordinates != null) {
                fieldsCoordinates.put(fieldEntry.getKey(), fieldCoordinates);
            }
        }
        return fieldsCoordinates;
    }
    
    public static Point getRelativeSpreadsheetFieldCoordinates(IOpenField field) {        
        if (field instanceof SpreadsheetCellField) {
            SpreadsheetCellField cellField = (SpreadsheetCellField) field;
            return cellField.getRelativeCoordinates();
        }
        return null;
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
