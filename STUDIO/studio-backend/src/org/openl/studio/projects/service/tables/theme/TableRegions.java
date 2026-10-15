package org.openl.studio.projects.service.tables.theme;

import java.util.List;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * The layout of one table: the regions its cells form, and the pieces of its header text.
 *
 * <p>A region is a rectangle of cells that take their look from the same parts of a theme. Regions are named in table
 * coordinates: the header row is row 0 and the first column of the table is column 0. They do not overlap. A cell no
 * region holds is one no theme reaches, and keeps its look.
 *
 * <p>The layout knows no theme, so it is laid out once for a compiled table and drawn in any theme.
 *
 * @param regions   the regions of the table
 * @param signature the pieces of the header text, in the order of the text, or an empty list when the header has no
 *                  keyword
 * @author Yury Molchan
 */
record TableRegions(List<Region> regions, List<HeaderPiece> signature) {

    TableRegions {
        regions = List.copyOf(regions);
        signature = List.copyOf(signature);
    }

    /**
     * A rectangle of cells taking their look from the same parts.
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
     * A piece of the header text.
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
