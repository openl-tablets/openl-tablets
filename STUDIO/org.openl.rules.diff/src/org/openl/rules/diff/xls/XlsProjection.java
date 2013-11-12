package org.openl.rules.diff.xls;

import java.util.List;

import org.openl.rules.diff.hierarchy.AbstractProjection;
import org.openl.rules.table.ICell;

public class XlsProjection extends AbstractProjection {
    private Object data;
    private List<ICell> diffCells;

    public XlsProjection(String name, XlsProjectionType type) {
        super(name, type.name());
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public List<ICell> getDiffCells() {
        return diffCells;
    }

    public void setDiffCells(List<ICell> diffCells) {
        this.diffCells = diffCells;
    }
}
