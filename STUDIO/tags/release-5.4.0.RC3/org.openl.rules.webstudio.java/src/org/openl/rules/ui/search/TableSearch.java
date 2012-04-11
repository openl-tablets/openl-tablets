package org.openl.rules.ui.search;

import org.openl.rules.table.ITable;

public class TableSearch {

    private String tableUri;
    private ITable table;
    private String xlsLink;

    public ITable getTable() {
        return table;
    }

    public String getTableUri() {
        return tableUri;
    }

    public String getXlsLink() {
        return xlsLink;
    }

    public void setTable(ITable table) {
        this.table = table;
    }

    public void setTableUri(String tableUri) {
        this.tableUri = tableUri;
    }

    public void setXlsLink(String xlsLink) {
        this.xlsLink = xlsLink;
    }

}
