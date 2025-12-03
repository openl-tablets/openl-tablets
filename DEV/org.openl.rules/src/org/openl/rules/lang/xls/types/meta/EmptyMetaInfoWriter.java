package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGridRegion;

public final class EmptyMetaInfoWriter implements MetaInfoWriter {

    private static final EmptyMetaInfoWriter INSTANCE = new EmptyMetaInfoWriter();

    public static EmptyMetaInfoWriter getInstance() {
        return INSTANCE;
    }

    private EmptyMetaInfoWriter() {
    }

    @Override
    public void setMetaInfo(int row, int col, CellMetaInfo metaInfo) {
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
