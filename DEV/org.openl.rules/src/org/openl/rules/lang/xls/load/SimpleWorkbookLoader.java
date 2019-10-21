package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.SpreadsheetConstants;

/**
 * Stores the strong reference to previously loaded Workbook instance and is not unloaded.
 */
public class SimpleWorkbookLoader implements WorkbookLoader {
    private final Workbook workbook;
    private final SpreadsheetConstants spreadsheetConstants;

    public SimpleWorkbookLoader(Workbook workbook) {
        this.workbook = workbook;
        this.spreadsheetConstants = new SpreadsheetConstants(workbook.getSpreadsheetVersion());
    }

    /**
     * Get the workbook. When this method is repeatedly called, always returns the same instance of workbook java
     * object.
     *
     * @return previously loaded workbook
     * @see #isCanUnload()
     */
    @Override
    public Workbook getWorkbook() {
        return workbook;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SheetLoader getSheetLoader(int sheetIndex) {
        return new SimpleSheetLoader(workbook.getSheetAt(sheetIndex));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSheets() {
        return workbook.getNumberOfSheets();
    }

    /**
     * Always returns false
     *
     * @return false
     */
    @Override
    public boolean isCanUnload() {
        return false;
    }

    /**
     * Has no effect because this implementation never unload previously loaded Workbook
     *
     * @param canUnload the flag that this workbook can or cannot be unloaded
     */
    @Override
    public void setCanUnload(boolean canUnload) {
    }

    @Override
    public SpreadsheetConstants getSpreadsheetConstants() {
        return spreadsheetConstants;
    }
}
