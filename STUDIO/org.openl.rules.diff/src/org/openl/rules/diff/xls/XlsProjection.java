package org.openl.rules.diff.xls;

import java.util.List;

import org.openl.rules.diff.hierarchy.AbstractProjection;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IOpenLTable;

public class XlsProjection extends AbstractProjection {
    private IOpenLTable table;
    private List<ICell> diffCells;

    public XlsProjection(String name, XlsProjectionType type) {
        super(name, type.name());
    }

    public IOpenLTable getTable() {
        return table;
    }

    public void setTable(IOpenLTable table) {
        this.table = table;
    }

    public List<ICell> getDiffCells() {
        return diffCells;
    }

    public void setDiffCells(List<ICell> diffCells) {
        this.diffCells = diffCells;
    }
}
