package org.openl.rules.calc;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.ILogicalTable;

public final class StubSpreadSheetResult extends SpreadsheetResult {
    private static final long serialVersionUID = 1L;

    private Map<String, Object> values = new HashMap<>();

    @Override
    public int getHeight() {
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
    public int getWidth() {
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
    public Object getValue(int row, int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void setValue(int row, int column, Object value) {
        throw new UnsupportedOperationException();
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
    public void setFieldValue(String name, Object value) {
        values.put(name, value);
    }

    @Override
    public Object getFieldValue(String name) {
        return values.get(name);
    }

    @Override
    public boolean hasField(String name) {
        return true;
    }

    @Override
    public String toString() {
        return "Stub SpreadsheetResult:\n" + values.toString();
    }
}
