package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.Cells;

public class LazyCells extends BaseLazyItem<Cells> {
    private final String workbookName;
    private final String sheetName;

    public LazyCells(File file, String entryName, String workbookName, String sheetName) {
        super(file, entryName);
        this.workbookName = workbookName;
        this.sheetName = sheetName;
    }

    public List<Cell> getCells() {
        return getInfo().getCells();
    }

    @Override
    protected void postProcess(Cells info) {
        for (Cell cell : info.getCells()) {
            if (cell.getNode() == null) {
                throw new IllegalArgumentException("Cell node isn't initialized");
            }
            cell.getNode().configure(workbookName, sheetName);
        }
    }
}
