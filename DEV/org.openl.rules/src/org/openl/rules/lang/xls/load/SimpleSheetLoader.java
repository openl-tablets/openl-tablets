package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.table.xls.PoiExcelHelper;

/**
 * Stores the strong reference to previously loaded Sheet instance and is not unloaded.
 */
public class SimpleSheetLoader implements SheetLoader {
    private final Sheet sheet;
    private String sheetName;

    public SimpleSheetLoader(Sheet sheet) {
        this.sheet = sheet;
    }

    /**
     * Get the sheet. When this method is repeatedly called, always returns the same instance of Sheet java object.
     *
     * @return previously loaded sheet
     */
    @Override
    public Sheet getSheet() {
        return sheet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSheetName() {
        if (sheetName == null) {
            sheetName = sheet.getSheetName();
        }
        return sheetName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CellLoader getCellLoader(int column, int row) {
        Cell cell = PoiExcelHelper.getCell(column, row, sheet);
        return cell == null ? NullCellLoader.INSTANCE : new SimpleCellLoader(cell);
    }
}
