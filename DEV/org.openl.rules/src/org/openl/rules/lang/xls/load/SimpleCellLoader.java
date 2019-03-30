package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Cell;

/**
 * Stores the strong reference to previously loaded Cell instance and is not unloaded.
 */
public class SimpleCellLoader implements CellLoader {
    private final Cell cell;

    public SimpleCellLoader(Cell cell) {
        this.cell = cell;
    }

    /**
     * Get the cell. When this method is repeatedly called, always returns the same instance of Cell java object.
     *
     * @return previously loaded cell
     */
    @Override
    public Cell getCell() {
        return cell;
    }
}
