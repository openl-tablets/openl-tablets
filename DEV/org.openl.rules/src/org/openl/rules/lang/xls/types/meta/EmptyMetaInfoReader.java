package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;

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
}
