package org.openl.rules.diff.xls;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.diff.hierarchy.AbstractProjection;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IOpenLTable;

public class XlsProjection extends AbstractProjection {
    @Getter
    @Setter
    private IOpenLTable table;
    @Getter
    @Setter
    private List<ICell> diffCells;

    public XlsProjection(String name, XlsProjectionType type) {
        super(name, type.name());
    }
}
