package org.openl.rules.calc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;

/**
 * Serializable bean that handles result of spreadsheet calculation.
 *
 */
public class SpreadsheetResult implements Serializable {
    
    private static final long serialVersionUID = 8704762477153429384L;
    
    private Object[][] results;
    private int height;
    private int width;
    private String[] columnNames;
    private String[] rowNames;
    private Map<String, Point> fieldsCoordinates;
    
    /**
     * logical representation of calculated spreadsheet table
     * it is needed for web studio to display results
     */
    private transient ILogicalTable logicalTable;
    
    public SpreadsheetResult(Object[][] results, String[] rowNames, String[] columnNames, 
            Map<String, Point> fieldsCoordinates) {
        this.columnNames = columnNames;
        this.rowNames = rowNames;
        this.height = rowNames.length;
        this.width = columnNames.length;
        this.results = results.clone();
        this.fieldsCoordinates = new HashMap<String, Point>(fieldsCoordinates);        
    }
    
    @Deprecated
    public int height() {
        return getHeight();
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public Object[][] getResults() {
        return results.clone();
    }

    public void setResults(Object[][] results) {
        this.results = results.clone();
    }
    
    @Deprecated
    public int width() {
        return getWidth();
    }
    
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String[] getColumnNames() {        
        return columnNames.clone();
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames.clone();
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames.clone();
    }    
    
    public Object getValue(int row, int column) {       
        return results[row][column];
    }
    
    public String getColumnName(int column) {
        return columnNames[column];                
    }    
    
    public String getRowName(int row) {
        return rowNames[row];        
    }
    
    public Map<String, Point> getFieldsCoordinates() {
        return new HashMap<String, Point>(fieldsCoordinates);
    }

    public void setFieldsCoordinates(Map<String, Point> fieldsCoordinates) {
        this.fieldsCoordinates = new HashMap<String, Point>(fieldsCoordinates);
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
        Point fieldCoordinates = fieldsCoordinates.get(name);        
        
        if (fieldCoordinates != null) {
            return getValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn());
        }
        return null;        
    }
}
