package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Cell;

public enum NullCellLoader implements CellLoader {
    INSTANCE;

    @Override
    public Cell getCell() {
        return null;
    }
}
