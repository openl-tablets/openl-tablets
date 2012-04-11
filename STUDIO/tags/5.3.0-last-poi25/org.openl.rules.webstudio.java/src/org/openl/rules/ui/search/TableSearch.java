package org.openl.rules.ui.search;

import org.openl.rules.table.IGridTable;

public class TableSearch {

    private String tableUri;
    private IGridTable table;
    private String xlsLink;

    public IGridTable getTable() {
        return table;
    }

    public String getTableUri() {
        return tableUri;
    }

    public String getXlsLink() {
        return xlsLink;
    }

    public void setTable(IGridTable table) {
        this.table = table;
    }

    public void setTableUri(String tableUri) {
        this.tableUri = tableUri;
    }

    public void setXlsLink(String xlsLink) {
        this.xlsLink = xlsLink;
    }

}
