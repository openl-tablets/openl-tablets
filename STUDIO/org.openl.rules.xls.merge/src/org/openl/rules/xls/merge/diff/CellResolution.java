package org.openl.rules.xls.merge.diff;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * How a table conflict is resolved cell by cell: the cells only one revision changed, or both changed alike, are
 * merged by themselves, and the revision a conflicting cell takes its value from is chosen for the cell or for the
 * whole table. Any cell of the merged table may take a typed value instead.
 *
 * <p>The cells are named by their row and their column in the merged table, counted from 0. A choice for a cell
 * overrides the choice for the table. A typed value overrides every choice.
 *
 * <p>A conflicting cell takes from the revision chosen the parts both revisions changed differently: its value and its
 * comment. It takes its style the way any merged cell does, and so does a cell with a typed value.
 *
 * @param table  the revision every conflicting cell of the table takes, or {@code null} when none is chosen for it
 * @param cells  the revisions chosen for single cells
 * @param values the values typed for cells, read as Excel reads what is typed into a cell
 */
public record CellResolution(@Nullable Resolution table,
                             Map<TableCell, Resolution> cells,
                             Map<TableCell, String> values) {

    /**
     * @throws IllegalArgumentException when BASE revision is chosen: a conflicting cell takes its value from OUR or
     *                                  THEIR revision
     */
    public CellResolution {
        cells = Map.copyOf(cells);
        values = Map.copyOf(values);
        if (table == Resolution.BASE || cells.containsValue(Resolution.BASE)) {
            throw new IllegalArgumentException("A conflicting cell takes its value from OUR or THEIR revision.");
        }
    }

    /**
     * The revision a conflicting cell takes, chosen for it or for the table.
     *
     * @return the revision, or {@code null} when none is chosen
     */
    public @Nullable Resolution sideOf(TableCell cell) {
        var chosen = cells.get(cell);
        return chosen != null ? chosen : table;
    }
}
