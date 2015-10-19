package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.single.ExtensionModuleInfo;
import org.openl.extension.xmlrules.model.single.SheetInfo;
import org.openl.extension.xmlrules.model.single.WorkbookInfo;

public class LazyExtensionModule extends BaseLazyItem<ExtensionModuleInfo> implements ExtensionModule {

    public static final String TYPES_WORKBOOK = "Types.xlsx";
    public static final String MAIN_WORKBOOK = "Main.xlsx";

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
        workbooks.add(new LazyWorkbook(getFile(), "", createMainWorkbook()));
        workbooks.add(new LazyWorkbook(getFile(), "", createTypeWorkbook()));

        return workbooks;
    }

    @Override
    public List<LazyWorkbook> getInternalWorkbooks() {
        List<LazyWorkbook> workbooks = new ArrayList<LazyWorkbook>();
        ExtensionModuleInfo info = getInfo();
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
        typeWorkbook.setXlsFileName(TYPES_WORKBOOK);
        return typeWorkbook;
    }

    private WorkbookInfo createMainWorkbook() {
        WorkbookInfo typeWorkbook = new WorkbookInfo();
        typeWorkbook.setXlsFileName(MAIN_WORKBOOK);
        return typeWorkbook;
    }
}
