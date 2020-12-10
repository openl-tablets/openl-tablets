package org.openl.rules.cmatch;

public class TableColumn {
    private final String id;
    private final int columnIndex;

    public TableColumn(String id, int columnIndex) {
        this.columnIndex = columnIndex;
        this.id = id;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public String getId() {
        return id;
    }
}
