package org.openl.studio.projects.service.tables.theme;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableValueType;

/**
 * Reads the role and the type of a cell of a table, named by its place on the sheet, from the regions the table is
 * laid out in.
 *
 * @param table   the table
 * @param regions its regions
 * @author Yury Molchan
 */
record Roles(IOpenLTable table, TableRegions regions) {

    /** The regions of a table, laid out afresh. */
    static Roles of(IOpenLTable table) {
        return new Roles(table, Objects.requireNonNull(TableRegionLayout.of(table, table.getGridTable())));
    }

    /** The role of a cell of the sheet. */
    RawTableRole roleAt(int row, int column) {
        return regionAt(row, column).role();
    }

    /** The type of the value of a cell of the sheet: its own, or the type of its region. */
    @Nullable
    RawTableValueType typeAt(int row, int column) {
        var region = table.getGridTable().getRegion();
        var own = regions.typeAt(row - region.getTop(), column - region.getLeft());
        return own != null ? own : regionAt(row, column).type();
    }

    private RawTableRegion regionAt(int row, int column) {
        var region = table.getGridTable().getRegion();
        return regions.regions()
                .stream()
                .filter(held -> held.holds(row - region.getTop(), column - region.getLeft()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No region holds the cell at " + row + ", " + column));
    }
}
