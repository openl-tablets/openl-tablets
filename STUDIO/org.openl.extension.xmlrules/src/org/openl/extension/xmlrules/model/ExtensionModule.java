package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.WorkbookInfo;

public interface ExtensionModule {
    String getFileName();

    List<LazyWorkbook> getWorkbooks();
}
