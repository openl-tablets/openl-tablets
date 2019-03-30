package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Sheet;

/**
 * Provides access to the Sheet.
 */
public interface SheetLoader {
    /**
     * Get the sheet. Depending on implementation, when this method is repeatedly called, it can (but mustn't) return
     * the different instances of Sheet java object.
     *
     * @return loaded sheet
     */
    Sheet getSheet();

    /**
     * Get the sheet name
     *
     * @return sheet name
     */
    String getSheetName();

    /**
     * Get the cell accessor of current sheet
     *
     * @param column the column of current sheet
     * @param row the row of current sheet
     * @return the cell accessor of current sheet
     */
    CellLoader getCellLoader(int column, int row);
}
