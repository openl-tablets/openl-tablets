package org.openl.studio.projects.service.tables.theme;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
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
 * column 0. They do not overlap and cover the table. The layout knows no theme, so it is laid out once for a compiled
 * table and serves every read of it.
 *
 * @param regions   the regions of the roles of the table
 * @param groups    the lines of the body that start a group
 * @param signature the pieces of the header text, or an empty list when the header has no keyword
 * @param types     the type of each value of a type other than the type of its region, by its place in the table
 * @param theme     how a table theme is laid over the table when it is written into a workbook
 * @author Yury Molchan
 */
public record TableRegions(List<RawTableRegion> regions,
                           RawTableGroups groups,
                           List<RawTableSignaturePiece> signature,
                           Map<Cell, RawTableValueType> types,
                           Theme theme) {

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
     * A cell of the table.
     *
     * @param row    the row, in table coordinates
     * @param column the column, in table coordinates
     */
    public record Cell(int row, int column) {
    }

    /**
     * How a table theme is laid over a table: the regions of the parts of the theme, and the pieces of the header the
     * theme formats.
     *
     * @param regions the regions of the parts of a theme, which cover the table
     * @param header  the pieces of the header text, in the order of the text, or an empty list when the header has no
     *                keyword
     */
    record Theme(List<Region> regions, List<HeaderPiece> header) {

        Theme {
            regions = List.copyOf(regions);
            header = List.copyOf(header);
        }
    }

    /**
     * A rectangle of cells taking their look from the same parts of a theme.
     *
     * @param parts  the parts of a theme the cells take their look from
     * @param top    the first row, in table coordinates
     * @param left   the first column, in table coordinates
     * @param bottom the last row, in table coordinates
     * @param right  the last column, in table coordinates
     */
    record Region(Parts parts, int top, int left, int bottom, int right) {

        /** Whether the region holds a cell, named in table coordinates. */
        boolean holds(int row, int column) {
            return top <= row && row <= bottom && left <= column && column <= right;
        }
    }

    /**
     * A piece of the header text, as a theme formats it.
     *
     * @param part  the part of the header the piece is
     * @param start the index of the first character of the piece
     * @param end   the index after the last character of the piece
     */
    record HeaderPiece(HeaderPart part, int start, int end) {
    }

    /** A part of the header text, which a theme gives a look of its own. */
    @RequiredArgsConstructor
    enum HeaderPart {
        /** The text between the other parts, such as spaces, in the look of the header cell. */
        TEXT(header -> null),
        /** The keyword, such as {@code Datatype}, {@code Spreadsheet} or {@code Rules}. */
        KEYWORD(TableTheme.Header::keyword),
        /** The name of the table. */
        NAME(TableTheme.Header::name),
        /** The type the header names besides the table, such as the type a Spreadsheet returns. */
        TYPE(TableTheme.Header::type),
        /** The parameters of a Spreadsheet or a decision table. */
        PARAMETERS(TableTheme.Header::parameters);

        private final Function<TableTheme.Header, @Nullable ThemeStyle> look;

        /**
         * The look of the part.
         *
         * @param cell   the look of the header cell, which every part starts from
         * @param header the look of each part of the header
         * @return the look of the part
         */
        ThemeStyle in(ThemeStyle cell, TableTheme.Header header) {
            return cell.with(look.apply(header));
        }
    }
}
