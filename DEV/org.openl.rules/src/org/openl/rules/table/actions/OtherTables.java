package org.openl.rules.table.actions;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * The tables of a sheet besides the one an edit changes: as that table grows, it must leave room around them.
 *
 * <p>Finding the tables reads the whole sheet, so they are found once, when the edit first asks for room, and kept
 * for every later insert of the same edit. An edit writes only its own table, moving it included, so the other
 * tables stay where they were found. Keep one for a single edit of a single table, never across edits.
 *
 * <p>The edited table is never one of them, whatever its size when they were found: its own cells never stand in
 * the way of its growth.
 *
 * @author Yury Molchan
 */
public final class OtherTables {

    private final IGridTable table;
    private @Nullable List<IGridRegion> regions;

    /**
     * @param table the table the edit changes
     */
    public OtherTables(IGridTable table) {
        this.table = table;
    }

    /**
     * Whether the region crosses none of the other tables.
     */
    boolean isFree(IGridRegion region) {
        return regions().stream().noneMatch(other -> GridRegionUtils.intersects(region, other));
    }

    private List<IGridRegion> regions() {
        if (regions == null) {
            var own = UndoableEditTableAction.getOriginalRegion(table);
            regions = Arrays.stream(table.getGrid().getTables())
                    .map(IGridTable::getRegion)
                    .filter(other -> !GridRegionUtils.intersects(other, own))
                    .toList();
        }
        return regions;
    }
}
