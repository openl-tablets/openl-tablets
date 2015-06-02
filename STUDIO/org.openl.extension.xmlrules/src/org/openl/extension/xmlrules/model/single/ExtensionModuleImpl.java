package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.*;

public class ExtensionModuleImpl implements ExtensionModule {
    private String xlsFileName;
    private List<TableGroup> tableGroups = new ArrayList<TableGroup>();

    @Override
    public String getXlsFileName() {
        return xlsFileName;
    }

    public void setXlsFileName(String xlsFileName) {
        this.xlsFileName = xlsFileName;
    }

    @Override
    public List<TableGroup> getTableGroups() {
        return tableGroups;
    }

    public void setTableGroups(List<TableGroup> tableGroups) {
        this.tableGroups = tableGroups;
    }
}
