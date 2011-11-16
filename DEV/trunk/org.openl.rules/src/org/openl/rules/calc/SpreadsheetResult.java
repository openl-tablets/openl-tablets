package org.openl.rules.calc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.helpers.ITableAdaptor;
import org.openl.rules.helpers.TablePrinter;
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
    
    public SpreadsheetResult(int height, int width) {
        this.height = height;
        this.width = width;
        this.results = new Object[height][width];        
        this.fieldsCoordinates = new HashMap<String, Point>();  
    }
    
    public SpreadsheetResult(Object[][] results, String[] rowNames, String[] columnNames, 
            Map<String, Point> fieldsCoordinates) {
        this.columnNames = columnNames;
        this.rowNames = rowNames;
        this.height = rowNames.length;
        this.width = columnNames.length;
        this.results = results.clone();
        this.fieldsCoordinates = new HashMap<String, Point>(fieldsCoordinates);        
    }
    
    /**
     * @deprecated
     * use {@link SpreadsheetResult#getHeight()} instead.
     * 
     */
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
    
    /**
     * @deprecated
     * use {@link SpreadsheetResult#getWidth()} instead.
     * 
     */
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
    
    protected void setValue(int row, int column, Object value) {
        results[row][column] = value;
    }
    
    public String getColumnName(int column) {
        if (columnNames != null) {
            return columnNames[column];
        }
        return "DefaultColumnName" + column;
    }    
    
    public String getRowName(int row) {
        if (rowNames != null) {
            return rowNames[row];
        }           
        return "DefaultRowName" + row;
    }
    
    public Map<String, Point> getFieldsCoordinates() {
        return new HashMap<String, Point>(fieldsCoordinates);
    }
    
    protected void addFieldCoordinates(String field, Point coord) {
        fieldsCoordinates.put(field, coord);
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
    
    
    public ITableAdaptor makeTableAdaptor()
    {
        ITableAdaptor asTableAdaptor = new ITableAdaptor() {
            
            public int width(int row) {
                return getWidth() + 1;
            }
            
            public int maxWidth() {
                return getWidth() + 1;
            }
            
            public int height() {
                return getHeight() + 1;
            }
            
            public Object get(int col, int row) {
                if (col == 0 && row == 0)
                        return "-X-";
                if (col == 0)
                    return getRowName(row-1);
                if (row == 0)
                    return getColumnName(col-1);
                
                return getValue(row-1, col-1);
            }
        };
        
        return asTableAdaptor;
    }
    
    public String printAsTable()
    {
        
        String res = new  TablePrinter(makeTableAdaptor(), null, " | ").print();
        return res;
    }

    @Override
    public String toString() {
        return printAsTable();
    }
    
    
    
}
