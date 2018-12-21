package org.openl.rules.calc.result;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
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

    public static Map<String, Point> getAbsoluteSpreadsheetFieldCoordinates(SpreadsheetResult spreadsheetResult) {
        Map<String, Point> absoluteCoordinates = new HashMap<String, Point>();

        IGridTable sourceTable = spreadsheetResult.getLogicalTable().getSource();
        
        String[] rowNames = spreadsheetResult.getRowNames();
        String[] columnNames = spreadsheetResult.getColumnNames();
        
        for (int i = 0; i < rowNames.length; i++) {
            for (int j = 0; j < columnNames.length; j++) {
                int column = getColumn(sourceTable, j);
                int row = getRow(sourceTable, i);
                ICell cell = sourceTable.getCell(column, row);
                Point absolute = new Point(cell.getAbsoluteColumn(), cell.getAbsoluteRow());
                StringBuilder sb = new StringBuilder();
                sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                    .append(columnNames[j])
                    .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                    .append(rowNames[i]);
                absoluteCoordinates.put(sb.toString(), absolute);
            }
        }

        return absoluteCoordinates;
    }

    /**
     * Get the column of a field in Spreadsheet.
     * 
     * @return column number
     */
    private static int getColumn(IGridTable spreadsheet, int columnFieldNumber) {
        int column = 0;
        // The column 0 contains row headers that's why "<=" instead of "<"
        for (int i = 0; i <= columnFieldNumber; i++) {
            column += spreadsheet.getCell(i, 0).getWidth();
        }
        return column;
    }

    /**
     * Get the row of a field in Spreadsheet.
     * 
     * @return row number
     */
    private static int getRow(IGridTable spreadsheet, int rowFieldNumber) {
        int row = 0;
        // The row 0 contains column headers that's why "<=" instead of "<"
        for (int i = 0; i <= rowFieldNumber; i++) {
            row += spreadsheet.getCell(0, i).getHeight();
        }
        return row;
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
