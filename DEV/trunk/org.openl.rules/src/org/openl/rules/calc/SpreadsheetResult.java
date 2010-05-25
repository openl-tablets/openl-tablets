package org.openl.rules.calc;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenField;

public class SpreadsheetResult {
    
    private Object[][] results;
    private int height;
    private int width;
    private String[] columnNames;
    private String[] rowNames;
    private Map<String, IOpenField> fields;
    
    /**
     * logical representation of calculated spreadsheet table
     * it is needed for web studio to display results
     */
    private ILogicalTable logicalTable;
    
    public SpreadsheetResult(Object[][] results, String[] rowNames, String[] columnNames, 
            Map<String, IOpenField> fields) {
        this.columnNames = columnNames;
        this.rowNames = rowNames;
        this.height = rowNames.length;
        this.width = columnNames.length;
        this.results = results.clone();
        this.fields = new HashMap<String, IOpenField>(fields);        
    }
    
    public int height() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public Object[][] getResults() {
        return results;
    }

    public void setResults(Object[][] results) {
        this.results = results;
    }
    
    public int width() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String[] getColumnNames() {        
        return columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }    
    
    public Object getValue(int row, int column) {       
        return results[row][column];
    }
    
    public String getColumnName(int column) {    
        if (column < columnNames.length) {
            return columnNames[column];
        }
        return null;        
    }    
    
    public String getRowName(int row) {
        if (row < rowNames.length) {
            return rowNames[row];
        } 
        return null;
    }
    
    public Map<String, IOpenField> getFields() {
        return new HashMap<String, IOpenField>(fields);
    }

    public void setFields(Map<String, IOpenField> fields) {
        this.fields = new HashMap<String, IOpenField>(fields);
    }
    
    /**
     * 
     * @return logical representation of calculated spreadsheet table
     * it is needed for web studio to display results
     */
    public ILogicalTable getLogicalTable() {
        return logicalTable;
    }

    public void setLogicalTable(ILogicalTable logicalTable) {
        this.logicalTable = logicalTable;
    }

    public Object getFieldValue(String name) {
        IOpenField field = fields.get(name);        
        
        if (field != null) {
            SpreadsheetCellField cellField = (SpreadsheetCellField) field;

            int row = cellField.getCell().getRowIndex();
            int column = cellField.getCell().getColumnIndex();

            return getValue(row, column);
        }
        return null;        
    }
}
