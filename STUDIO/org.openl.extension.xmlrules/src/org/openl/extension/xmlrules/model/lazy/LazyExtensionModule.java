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
        ExtensionModuleInfo info = getInfo();
        if (info == null) {
            throw new IllegalArgumentException("There is no " + getEntryName() + " file");
        }
        for (WorkbookInfo workbookInfo : info.getWorkbooks()) {
            workbooks.add(new LazyWorkbook(getFile(), "", workbookInfo));
        }
        WorkbookInfo typeWorkbook = createTypeWorkbook();
        workbooks.add(new LazyWorkbook(getFile(), "", typeWorkbook));

        return workbooks;
    }

    private WorkbookInfo createTypeWorkbook() {
        WorkbookInfo typeWorkbook = new WorkbookInfo();
        typeWorkbook.setXlsFileName(TYPES_WORKBOOK);
        ArrayList<SheetInfo> sheets = new ArrayList<SheetInfo>();
        SheetInfo sheetInfo = new SheetInfo();
        sheetInfo.setName("Types");
        sheets.add(sheetInfo);
        typeWorkbook.setSheets(sheets);
        return typeWorkbook;
    }
}
