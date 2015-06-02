package org.openl.extension.xmlrules.model;

import java.util.List;

public interface ExtensionModule {
    String getXlsFileName();

    List<TableGroup> getTableGroups();
}
