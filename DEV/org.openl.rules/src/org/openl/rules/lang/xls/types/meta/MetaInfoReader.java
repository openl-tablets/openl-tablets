package org.openl.rules.lang.xls.types.meta;

import java.util.List;

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
     * Preliminary load meta info for a given region. For a big tables it's a part of the table, meta info for full
     * table not needed in this case. If this method wasn't invoked before {@link #getMetaInfo(int, int)}, then meta
     * info for full table can be loaded.
     *
     * @param region region to load meta info
     */
    void prepare(IGridRegion region);

    /**
     * Release meta info loaded in {@link #prepare(IGridRegion)}
     */
    void release();

    /**
     * The parts of the table whose every cell holds the same thing, read from what the table declares rather
     * than from what stands in it.
     *
     * <p>Answered for the whole table, by the table's own rows and columns. A kind that says nothing about a
     * cell until something is written in it answers with an empty list.
     */
    default List<TableArea> getAreas() {
        return List.of();
    }
}
