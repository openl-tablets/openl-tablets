package org.openl.rules.ui.search;

import org.openl.rules.table.IGridTable;

public class TableSearch {

    private int tableId;
    private IGridTable table;
    private String xlsLink;

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public IGridTable getTable() {
        return table;
    }

    public void setTable(IGridTable table) {
        this.table = table;
    }

    public String getXlsLink() {
        return xlsLink;
    }

    public void setXlsLink(String xlsLink) {
        this.xlsLink = xlsLink;
    }

}
