package org.openl.extension.xmlrules.model.lazy;

import java.io.File;
import java.util.List;

import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.Cells;
import org.openl.extension.xmlrules.model.single.node.NamedRange;

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

    public List<NamedRange> getNamedRanges() {
        return getInfo().getNamedRanges();
    }

    @Override
    protected void postProcess(Cells info) {
        if (info == null) {
            return;
        }
        for (Cell cell : info.getCells()) {
            if (cell.getNode() == null) {
                return;
            }
            cell.getNode().configure(workbookName, sheetName, cell);
        }
    }
}
