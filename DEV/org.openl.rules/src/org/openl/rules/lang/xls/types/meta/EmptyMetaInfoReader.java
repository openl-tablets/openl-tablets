package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGridRegion;

public final class EmptyMetaInfoReader implements MetaInfoReader {
    private static EmptyMetaInfoReader ourInstance = new EmptyMetaInfoReader();

    public static EmptyMetaInfoReader getInstance() {
        return ourInstance;
    }

    private EmptyMetaInfoReader() {
    }

    @Override
    public CellMetaInfo getMetaInfo(int row, int col) {
        return null;
    }

    @Override
    public void prepare(IGridRegion region) {
    }

    @Override
    public void release() {
    }
}
