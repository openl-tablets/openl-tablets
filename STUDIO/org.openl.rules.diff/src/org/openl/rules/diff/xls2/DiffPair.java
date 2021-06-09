package org.openl.rules.diff.xls2;

import java.util.List;

import org.openl.rules.table.ICell;

public class DiffPair {
    private final XlsTable table1;
    private final XlsTable table2;
    private List<ICell> diffCells1;
    private List<ICell> diffCells2;

    public DiffPair(XlsTable table1, XlsTable table2) {
        this.table1 = table1;
        this.table2 = table2;
    }

    public XlsTable getTable1() {
        return table1;
    }

    public XlsTable getTable2() {
        return table2;
    }

    public List<ICell> getDiffCells1() {
        return diffCells1;
    }

    public void setDiffCells1(List<ICell> diffCells) {
        this.diffCells1 = diffCells;
    }

    public List<ICell> getDiffCells2() {
        return diffCells2;
    }

    public void setDiffCells2(List<ICell> diffCells) {
        this.diffCells2 = diffCells;
    }
}
