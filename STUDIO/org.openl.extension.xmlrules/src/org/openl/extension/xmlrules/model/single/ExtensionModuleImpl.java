package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;

public class ExtensionModuleImpl implements ExtensionModule {
    private String xlsFileName;
    private List<LazyWorkbook> workbooks = new ArrayList<LazyWorkbook>();

    @Override
    public String getFileName() {
        return xlsFileName;
    }

    public void setXlsFileName(String xlsFileName) {
        this.xlsFileName = xlsFileName;
    }

    @Override
    public List<LazyWorkbook> getWorkbooks() {
        return workbooks;
    }
}
