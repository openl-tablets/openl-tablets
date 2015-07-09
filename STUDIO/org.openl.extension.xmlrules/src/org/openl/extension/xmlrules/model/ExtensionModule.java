package org.openl.extension.xmlrules.model;

import java.util.List;

public interface ExtensionModule {
    String getFormatVersion();
    String getXlsFileName();

    List<Sheet> getSheets();
}
