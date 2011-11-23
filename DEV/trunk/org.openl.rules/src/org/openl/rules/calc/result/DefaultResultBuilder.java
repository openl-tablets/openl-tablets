package org.openl.rules.calc.result;

import java.lang.reflect.Constructor;
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
        
        Map<String, Point> fieldsCoordinates = getFieldsCoordinates(result.getSpreadsheet().getSpreadsheetType().getFields());
        

        Constructor<?> constructor = null;
        try {
            constructor = result.getSpreadsheet().getType().getInstanceClass().getConstructor(Object[][].class, String[].class, String[].class, Map.class);
        } catch (Exception e1) {
            //TODO: add logger
        }
        
        SpreadsheetResult spreadsheetBean = null;
        try {
            spreadsheetBean = (SpreadsheetResult)constructor.newInstance(resultArray, rowNames, columnNames, fieldsCoordinates);
        } catch (Exception e) {
            //TODO: add logger
        } 
        
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
    
    public static Point getAbsoluteSpreadsheetFieldCoordinates(IOpenField field) {        
        if (field instanceof SpreadsheetCellField) {
            SpreadsheetCellField cellField = (SpreadsheetCellField) field;
            return cellField.getAbsoluteCoordinates();
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
