package org.openl.rules.lang.xls.load;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Provides lazy access to the Sheet. If corresponding workbook was garbage collected previously, it will bec loaded
 * again when {@link #getSheet()} is invoked.
 */
@RequiredArgsConstructor
public class LazySheetLoader implements SheetLoader {
    private final WorkbookLoader workbookLoader;
    private final int sheetIndex;

    private String sheetName;

    /**
     * Get the sheet. When this method is repeatedly called, it can (but mustn't) return the different instances of
     * Sheet java object.
     *
     * @return loaded sheet
     */
    @Override
    public Sheet getSheet() {
        return workbookLoader.getWorkbook().getSheetAt(sheetIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSheetName() {
        if (sheetName == null) {
            sheetName = getSheet().getSheetName();
        }
        return sheetName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CellLoader getCellLoader(int column, int row) {
        return new LazyCellLoader(this, column, row);
    }
}
