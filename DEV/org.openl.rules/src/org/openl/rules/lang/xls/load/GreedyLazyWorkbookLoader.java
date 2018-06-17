package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Workbook;
import org.openl.source.IOpenSourceCodeModule;

/**
 * Lazily loads workbook only when it's needed but never unloads it
 */
public class GreedyLazyWorkbookLoader implements WorkbookLoader {

    private final IOpenSourceCodeModule fileSource;

    private Workbook workbook;

    public GreedyLazyWorkbookLoader(IOpenSourceCodeModule fileSource) {
        this.fileSource = fileSource;
    }

    @Override
    public Workbook getWorkbook() {
        if (workbook == null) {
            workbook = WorkbookLoadUtils.loadWorkbook(fileSource);
        }

        return workbook;
    }

    @Override
    public SheetLoader getSheetLoader(int sheetIndex) {
        return new LazySheetLoader(this, sheetIndex);
    }

    @Override
    public boolean isCanUnload() {
        return false;
    }

    @Override
    public void setCanUnload(boolean canUnload) {
    }

    @Override
    public int getNumberOfSheets() {
        return getWorkbook().getNumberOfSheets();
    }

}
