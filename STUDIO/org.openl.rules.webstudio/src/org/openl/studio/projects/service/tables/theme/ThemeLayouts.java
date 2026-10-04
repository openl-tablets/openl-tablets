package org.openl.studio.projects.service.tables.theme;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.theme.ThemedTable.Cell;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedCell;

/**
 * Decides which look a theme gives each cell of a table, whatever its kind.
 *
 * <p>The screen, the editor and the project-wide writer all ask here, so the screen shows what writing the theme
 * gives. The table is read as it stands on its grid, so a table being edited is themed with the rows and columns
 * the edit left it with.
 *
 * <p>The body of a Datatype and a Vocabulary is laid out by {@link DatatypeThemeLayout}, the body of a Spreadsheet by
 * {@link SpreadsheetThemeLayout}. A table of any other kind takes no theme.
 *
 * <p>Every kind themes its header the same way. The header look reaches across the table, and the cell holding the
 * header text formats it in pieces. The rows of properties a table declares between its header and its body get the
 * properties look.
 */
final class ThemeLayouts {

    private ThemeLayouts() {
    }

    /**
     * Whether a table is of a kind every theme styles: a Datatype, a Vocabulary or a Spreadsheet.
     *
     * @param table the table
     * @return {@code true} when a theme can be drawn over the table and written into it
     */
    static boolean styles(IOpenLTable table) {
        return OpenLTableUtils.isDatatypeTable(table) || OpenLTableUtils.isSpreadsheetTable(table);
    }

    /**
     * The look a theme gives each cell of a table.
     *
     * @param kind  the table, which tells its kind
     * @param table the table as it stands on its grid, header included
     * @param theme the theme
     * @return the look of each cell the theme reaches, or {@code null} for a table of a kind no theme styles
     */
    static @Nullable ThemedTable of(IOpenLTable kind, IGridTable table, TableTheme theme) {
        var datatype = OpenLTableUtils.isDatatypeTable(kind);
        if (!datatype && !OpenLTableUtils.isSpreadsheetTable(kind)) {
            return null;
        }
        var logical = LogicalTableHelper.logicalTable(table);
        var header = Objects.requireNonNullElse(logical.getCell(0, 0).getStringValue(), "");
        var vocabulary = datatype && OpenLTableUtils.isVocabularyHeader(header);
        var datatypeKind = vocabulary ? theme.vocabulary() : theme.datatype();
        var look = theme.lookOf(datatype ? datatypeKind : theme.spreadsheet());
        var base = ThemeStyle.NONE.with(look.style());
        var cells = themeHead(table, logical, base, look);
        var body = bodyOf(logical);
        if (body != null) {
            if (datatype) {
                DatatypeThemeLayout.themeBody(cells, body, kind, base, look, vocabulary);
            } else {
                SpreadsheetThemeLayout.themeBody(cells, body, header, base, look);
            }
        }
        // A region of empty cells does not widen the table, so it may be merged past the edge of the table: the cells
        // beyond the edge are not the table's.
        var region = table.getRegion();
        cells.keySet().removeIf(cell -> !GridRegionUtils.contains(region, cell.column(), cell.row()));
        return new ThemedTable(Collections.unmodifiableMap(cells));
    }

    /**
     * The look of the header row and of the properties under it, which the body of the table joins.
     *
     * @param table   the table as it stands on its grid
     * @param logical the same table read with its merged cells
     * @param base    the look every cell of the table starts from
     * @param look    the look of the table
     * @return the look of each cell of the header and the properties, in a map the body adds its cells to
     */
    private static Map<Cell, ThemedCell> themeHead(IGridTable table, ILogicalTable logical, ThemeStyle base,
                                                   TableTheme.Look look) {
        var header = look.header() == null ? TableTheme.Header.builder().build() : look.header();
        var style = base.with(header.style());
        var cells = new HashMap<Cell, ThemedCell>();
        // The header look reaches across the table, whether the header is merged over it or not.
        for (var column = 0; column < table.getWidth(); column++) {
            cover(cells, table.getCell(column, 0), style);
        }
        // Only the cell holding the text formats it in pieces; the rest of the merged header carries none.
        var text = logical.getCell(0, 0).getAbsoluteRegion();
        cells.put(new Cell(text.getTop(), text.getLeft()), new ThemedCell(style, header));
        if (PropertiesHelper.getPropertiesTableSection(logical) != null) {
            // The whole row of the section: the keyword, and the properties it names.
            themeProperties(cells, logical.getRows(1, 1).getSource(), base, base.with(look.properties()));
        }
        return cells;
    }

    /**
     * Gives the rows of table properties the properties look.
     *
     * <p>The section is one row of the table, though each property stands on a row of the sheet of its own. A line
     * the look draws above or below the properties goes round the whole section, not round each of its rows.
     */
    private static void themeProperties(Map<Cell, ThemedCell> cells, IGridTable rows, ThemeStyle base,
                                        ThemeStyle look) {
        var top = rows.getCell(0, 0).getAbsoluteRow();
        var bottom = top + rows.getHeight() - 1;
        for (var row = 0; row < rows.getHeight(); row++) {
            for (var column = 0; column < rows.getWidth(); column++) {
                var cell = rows.getCell(column, row);
                // A cell merged over several properties, such as the keyword, reaches the edges of all of them.
                var region = cell.getAbsoluteRegion();
                cover(cells, cell, look.atEdges(base, region.getTop() <= top, region.getBottom() >= bottom));
            }
        }
    }

    /**
     * The body of a table: its rows under the header and the properties it declares.
     *
     * @param logical the table read with its merged cells
     * @return the body, or {@code null} for a table that has none
     */
    private static @Nullable ILogicalTable bodyOf(ILogicalTable logical) {
        var bodyStart = PropertiesHelper.getPropertiesTableSection(logical) == null ? 1 : 2;
        return logical.getHeight() > bodyStart ? logical.getRows(bodyStart) : null;
    }

    /** Gives every cell of the region a cell covers the same look. */
    static void cover(Map<Cell, ThemedCell> cells, ICell cell, ThemeStyle style) {
        var region = cell.getAbsoluteRegion();
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var column = region.getLeft(); column <= region.getRight(); column++) {
                cells.put(new Cell(row, column), new ThemedCell(style, null));
            }
        }
    }
}
