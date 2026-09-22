package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGridRegion;

public final class EmptyMetaInfoReader implements MetaInfoReader {
    private static final EmptyMetaInfoReader INSTANCE = new EmptyMetaInfoReader();

    public static EmptyMetaInfoReader getInstance() {
        return INSTANCE;
    }

    private EmptyMetaInfoReader() {
    }

    @Override
    public CellMetaInfo getMetaInfo(int row, int col) {
        return null;
    }

    @Override
    public void prepare(IGridRegion region) {
        // There is no meta information behind this reader.
    }

    @Override
    public void release() {
        // There is no meta information behind this reader.
    }
}
