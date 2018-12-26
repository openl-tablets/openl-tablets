package org.openl.rules.calc;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.helpers.ITableAdaptor;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;

public class StubSpreadSheetResult extends SpreadsheetResult {
    private static final long serialVersionUID = 1L;

    private Map<String, Object> values = new HashMap<String, Object>();
    private Map<String, Point> fieldsCoordinates = new HashMap<String, Point>();

    private int lastRow = 0;

    public StubSpreadSheetResult() {
        super(0, 0);
    }

    @Override
    public int height() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHeight() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeight(int height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[][] getResults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResults(Object[][] results) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int width() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getWidth() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWidth(int width) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getColumnNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setColumnNames(String[] columnNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getRowNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRowNames(String[] rowNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getColumnTitles() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setColumnTitles(String[] columnTitles) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String[] getRowTitles() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setRowTitles(String[] rowTitles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getValue(int row, int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void setValue(int row, int column, Object value) {
        for (java.util.Map.Entry<String, Point> entry : fieldsCoordinates.entrySet()) {
            Point p = entry.getValue();
            if (p.getRow() == row && p.getColumn() == column) {
                String name = entry.getKey();
                values.put(name, value);
                return;
            }
        }
    }
    
    @Override
    public void setFieldValue(String name, Object value) {
        Point fieldCoordinates = fieldsCoordinates.get(name);

        if (fieldCoordinates != null) {
            setValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn(), value);
        }
    }

    @Override
    public String getColumnName(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRowName(int row) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ILogicalTable getLogicalTable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLogicalTable(ILogicalTable logicalTable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getFieldValue(String name) {
        return values.get(name);
    }

    @Override
    public boolean hasField(String name) {
        if (!fieldsCoordinates.containsKey(name)) {
            fieldsCoordinates.put(name, new Point(0, lastRow));
            lastRow++;
        }
        return true;
    }

    @Override
    public ITableAdaptor makeTableAdaptor() {
        return new ITableAdaptor() {
            public int width(int row) {
                return 2;
            }

            public int maxWidth() {
                return 2;
            }

            public int height() {
                return lastRow;
            }

            public Object get(int col, int row) {
                for (java.util.Map.Entry<String, Point> entry : fieldsCoordinates.entrySet()) {
                    Point p = entry.getValue();
                    if (p.getRow() == row) {
                        if (col == 0) {
                            return entry.getKey();
                        } else if (col == 1) {
                            return values.get(entry.getKey());
                        } else {
                            throw new IllegalArgumentException("Can't find column " + col);
                        }
                    }
                }
                throw new IllegalArgumentException("Can't find row " + row);
            }
        };
    }
}
