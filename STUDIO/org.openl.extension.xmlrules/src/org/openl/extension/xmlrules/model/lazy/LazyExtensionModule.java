package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.ExtensionDescriptor;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.single.ExtensionModuleInfo;
import org.openl.extension.xmlrules.model.single.WorkbookInfo;

public class LazyExtensionModule extends BaseLazyItem<ExtensionModuleInfo> implements ExtensionModule {

    public LazyExtensionModule(File file, String mainEntryName) {
        super(file, mainEntryName);
    }

    @Override
    public String getFileName() {
        return getFile().getName();
    }

    @Override
    public List<LazyWorkbook> getWorkbooks() {
        List<LazyWorkbook> workbooks = new ArrayList<LazyWorkbook>();
        workbooks.add(new LazyWorkbook(getFile(), "", createTypeWorkbook()));
        workbooks.add(new LazyWorkbook(getFile(), "", createMainWorkbook()));

        return workbooks;
    }

    @Override
    public List<LazyWorkbook> getInternalWorkbooks() {
        List<LazyWorkbook> workbooks = new ArrayList<LazyWorkbook>();
        ExtensionModuleInfo info = getInstance();
        if (info == null) {
            throw new IllegalArgumentException("There is no " + getEntryName() + " file");
        }
        for (WorkbookInfo workbookInfo : info.getWorkbooks()) {
            workbooks.add(new LazyWorkbook(getFile(), "", workbookInfo));
        }

        return workbooks;
    }

    private WorkbookInfo createTypeWorkbook() {
        WorkbookInfo typeWorkbook = new WorkbookInfo();
        typeWorkbook.setXlsFileName(ExtensionDescriptor.TYPES_WORKBOOK);
        return typeWorkbook;
    }

    private WorkbookInfo createMainWorkbook() {
        WorkbookInfo typeWorkbook = new WorkbookInfo();
        typeWorkbook.setXlsFileName(ExtensionDescriptor.MAIN_WORKBOOK);
        return typeWorkbook;
    }
}
