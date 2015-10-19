package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.SheetInfo;
import org.openl.extension.xmlrules.model.single.WorkbookInfo;

public class LazyWorkbook extends BaseLazyItem<WorkbookInfo> {
    private WorkbookInfo info;
    private List<Sheet> sheets;

    public LazyWorkbook(File file,
            String entryName,
            WorkbookInfo info) {
        super(file, entryName);
        this.info = info;
    }

    @Override
    public WorkbookInfo getInfo() {
        return info;
    }

    public String getXlsFileName() {
        return getInfo().getXlsFileName();
    }

    public List<Sheet> getSheets() {
        if (this.sheets != null) {
            return this.sheets;
        }
        List<Sheet> sheets = new ArrayList<Sheet>();
        List<SheetInfo> loadedSheets = getInfo().getSheets();
        for (int i = 0; i < loadedSheets.size(); i++) {
            loadedSheets.get(i).setId(i + 1);
        }
        for (final SheetInfo sheetInfo : loadedSheets) {
            sheets.add(new LazySheet(sheetInfo, getFile(), getXlsFileName()));
        }
        return sheets;
    }

    public void setSheets(List<Sheet> sheets) {
        this.sheets = sheets;
    }
}
