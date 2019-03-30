package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Cell;

/**
 * Provides access to the Cell.
 */
public interface CellLoader {
    /**
     * Get the cell. Depending on implementation, when this method is repeatedly called, it can (but mustn't) return the
     * different instances of Cell java object.
     *
     * @return loaded cell
     */
    Cell getCell();
}
