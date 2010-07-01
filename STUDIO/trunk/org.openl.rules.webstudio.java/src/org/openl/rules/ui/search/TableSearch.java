package org.openl.rules.ui.search;

import org.openl.rules.table.ITable;

@Deprecated
public class TableSearch {

    private ITable table;
    private String xlsLink;

    public ITable getTable() {
        return table;
    }

    public String getXlsLink() {
        return xlsLink;
    }

    public void setTable(ITable table) {
        this.table = table;
    }

    public void setXlsLink(String xlsLink) {
        this.xlsLink = xlsLink;
    }

}
