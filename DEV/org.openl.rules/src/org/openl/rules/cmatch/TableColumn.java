package org.openl.rules.cmatch;

import lombok.Getter;

public class TableColumn {
    @Getter
    private final String id;
    @Getter
    private final int columnIndex;

    public TableColumn(String id, int columnIndex) {
        this.columnIndex = columnIndex;
        this.id = id;
    }
}
