package org.openl.rules.ui.search;

import org.openl.rules.table.IGridTable;

public class TableSearch {

    private String tableUri;
    private IGridTable table;
    private String xlsLink;

    public String getTableUri() {
        return tableUri;
    }

    public void setTableUri(String tableUri) {
        this.tableUri = tableUri;
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
