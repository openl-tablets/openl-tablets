package org.openl.studio.projects.service.tables.format;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableSignaturePiece;
import org.openl.studio.projects.model.tables.RawTableValueType;

/**
 * The layout of one table: the regions of its roles, the lines starting a group, the pieces of its header text, and
 * the type of each value whose type differs from the type of its region.
 *
 * <p>The regions are named in table coordinates: the header row is row 0 and the first column of the table is
 * column 0. They do not overlap and cover the table. The layout is laid out once for a compiled table and serves every
 * read of it and every formatting of it.
 *
 * @param regions   the regions of the roles of the table
 * @param groups    the lines of the body that start a group
 * @param signature the pieces of the header text, or an empty list when the header has no keyword
 * @param types     the type of each value of a type other than the type of its region, by its place in the table
 * @author Yury Molchan
 */
public record TableRegions(List<RawTableRegion> regions,
                           RawTableGroups groups,
                           List<RawTableSignaturePiece> signature,
                           Map<Cell, RawTableValueType> types) {

    public TableRegions {
        regions = List.copyOf(regions);
        signature = List.copyOf(signature);
        types = Map.copyOf(types);
    }

    /**
     * The type of the value of a cell, when it differs from the type of its region.
     *
     * @param row    the row of the cell, in table coordinates
     * @param column the column of the cell, in table coordinates
     * @return the type, or {@code null} for a cell of the type of its region or holding no value of a type
     */
    public @Nullable RawTableValueType typeAt(int row, int column) {
        return types.get(new Cell(row, column));
    }

    /**
     * Whether the header text is written in its pieces: the pieces reach to its end. A header with no pieces, or with
     * pieces of another text, is written as plain text.
     *
     * @param header the header text
     * @return {@code true} when the header text is split into the pieces
     */
    boolean splits(String header) {
        return !signature.isEmpty() && signature.getLast().to() == header.length();
    }

    /**
     * A cell of the table.
     *
     * @param row    the row, in table coordinates
     * @param column the column, in table coordinates
     */
    public record Cell(int row, int column) {
    }
}
