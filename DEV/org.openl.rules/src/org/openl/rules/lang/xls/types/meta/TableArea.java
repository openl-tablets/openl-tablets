package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;

/**
 * A part of a table whose every cell holds the same thing, and what that is.
 *
 * <p>A table says what a part of it holds before anything is written there — the column a Data table declares,
 * the condition a decision table declares, the cells a lookup's rules meet in — so the part answers for a cell
 * nobody has written in yet and for one written into a line laid down later. A part reaching the table's edge
 * reaches on past it: a line laid down there belongs to it too.
 *
 * @param row      the table's own row the part begins at
 * @param column   the table's own column the part begins at
 * @param rows     how many rows it covers, or {@link #TO_THE_END}
 * @param columns  how many columns it covers, or {@link #TO_THE_END}
 * @param metaInfo what every cell of it holds
 */
public record TableArea(int row, int column, int rows, int columns, CellMetaInfo metaInfo) {

    /** Covers the rest of the table along that axis, and whatever is laid down beyond it. */
    public static final int TO_THE_END = -1;
}
