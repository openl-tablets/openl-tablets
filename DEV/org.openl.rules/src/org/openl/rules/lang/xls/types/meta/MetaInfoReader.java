package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGridRegion;

public interface MetaInfoReader {
    /**
     * Get meta info for a given row and column.
     *
     * @param row row
     * @param col column
     * @return meta info
     */
    CellMetaInfo getMetaInfo(int row, int col);

    /**
     * Preliminary load meta info for a given region.
     * For a big tables it's a part of the table, meta info for full table not needed in this case.
     * If this method wasn't invoked before {@link #getMetaInfo(int, int)}, then meta info for full table can be loaded.
     *
     * @param region region to load meta info
     */
    void prepare(IGridRegion region);

    /**
     * Release meta info loaded in {@link #prepare(IGridRegion)}
     */
    void release();
}
