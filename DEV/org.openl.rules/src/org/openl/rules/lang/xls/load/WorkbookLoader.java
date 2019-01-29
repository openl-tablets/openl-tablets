package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.SpreadsheetConstants;

/**
 * Provides access to the Workbook.
 */
public interface WorkbookLoader {
    /**
     * Get the workbook.
     * Depending on implementation and {@link #isCanUnload()} state, when this method is repeatedly called,
     * it can (but mustn't) return different instances of workbook java object.
     *
     * @return loaded workbook
     * @see #isCanUnload()
     */
    Workbook getWorkbook();

    /**
     * Get the sheet accessor
     *
     * @param sheetIndex the sheet index in the workbook
     * @return object that provides access to the specified sheet of current workbook
     */
    SheetLoader getSheetLoader(int sheetIndex);

    /**
     * Get number of sheets in current workbook.
     *
     * @return the number of sheets in current workbook
     */
    int getNumberOfSheets();

    /**
     * <p>
     * If true - the workbook can (but mustn't) be unloaded (for example if there is no enough memory).
     * In this case when the {@link #getWorkbook()} is repeatedly called, it can (but mustn't) return
     * different instances of workbook java object.
     * </p>
     * <p>
     * If false - the workbook is not unloaded and {@link #getWorkbook()} always returns the same Workbook instance.
     * </p>
     *
     * @return the flag that this workbook can or can't be unloaded.
     * @see #setCanUnload(boolean)
     */
    boolean isCanUnload();

    /**
     * Set the flag that this workbook can or can't be unloaded.
     * If workbook can't be unloaded (for example when we edit the workbook) internal implementation should always keep
     * strong reference to workbook and {@link #getWorkbook()} always returns the same Workbook instance.
     *
     * @param canUnload the flag that this workbook can or can't be unloaded
     * @see #isCanUnload()
     */
    void setCanUnload(boolean canUnload);

    SpreadsheetConstants getSpreadsheetConstants();
}
