package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Cell;
import org.openl.rules.table.xls.PoiExcelHelper;

import java.lang.ref.WeakReference;

/**
 * Provides lazy access to the Cell. If corresponding workbook was garbage collected previously, it will bec loaded
 * again when {@link #getCell()} is invoked.
 */
public class LazyCellLoader implements CellLoader {
    private final SheetLoader sheetLoader;
    private final int column;
    private final int row;

    private WeakReference<Cell> cellCache = new WeakReference<>(null);

    public LazyCellLoader(SheetLoader sheetLoader, int column, int row) {
        this.sheetLoader = sheetLoader;
        this.column = column;
        this.row = row;
    }

    /**
     * Get the cell. When this method is repeatedly called, it can (but mustn't) return the different instances of Cell
     * java object.
     *
     * @return loaded cell
     */
    @Override
    public Cell getCell() {
        if (cellCache == null) {
            return null;
        }
        Cell cell = cellCache.get();
        if (cell == null) {
            cell = PoiExcelHelper.getCell(column, row, sheetLoader.getSheet());
            cellCache = cell == null ? null : new WeakReference<>(cell);
        }
        return cell;
    }
}
