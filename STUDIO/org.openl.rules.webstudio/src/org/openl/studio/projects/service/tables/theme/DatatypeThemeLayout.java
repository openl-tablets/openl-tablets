package org.openl.studio.projects.service.tables.theme;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.studio.projects.model.tables.DatatypeLayout;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.theme.ThemedTable.Cell;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedCell;

/**
 * Decides which look a theme gives each cell of a Datatype or a Vocabulary table.
 *
 * <p>The screen, the editor and the project-wide writer all ask here, so the screen shows what writing the theme
 * gives. The table is read as it stands on its grid, so a table being edited is themed with the rows and columns
 * the edit left it with.
 *
 * <p>The header row gets the header look, and the text of the header is formatted in pieces. The rows below it get the look of their
 * column: the field types, the field names, or the other values. A Datatype that names its columns gets the title
 * look on that row. The last row gets the last-row look laid over its own. Every cell starts from the look of the
 * whole table. The properties a table declares between its header and its body keep their look.
 *
 * <p>A Datatype written transposed keeps a field in each column. It takes the looks the way it is compiled: the row
 * of field types takes the type look, the row of names takes the name look, and a column that names the rows takes
 * the title look. Its last row still closes the table. A table that did not compile is themed as it is written.
 */
final class DatatypeThemeLayout {

    private static final TableTheme.Header NO_HEADER = new TableTheme.Header(null, null, null, null);

    private DatatypeThemeLayout() {
    }

    /**
     * The look a theme gives each cell of a Datatype or a Vocabulary table.
     *
     * @param kind  the table, which tells its kind
     * @param table the table as it stands on its grid, header included
     * @param theme the theme
     * @return the look of each cell the theme reaches, or {@code null} for a table of any kind but a Datatype and
     * a table of a kind the theme names no look for
     */
    static @Nullable ThemedTable of(IOpenLTable kind, IGridTable table, TableTheme theme) {
        if (!OpenLTableUtils.isDatatypeTable(kind)) {
            return null;
        }
        var logical = LogicalTableHelper.logicalTable(table);
        var headerCell = logical.getCell(0, 0);
        var vocabulary = OpenLTableUtils.isVocabularyHeader(headerCell.getStringValue());
        var look = theme.lookOf(vocabulary);
        if (look == null) {
            return null;
        }
        var base = ThemeStyle.NONE.with(look.style());
        var header = look.header() == null ? NO_HEADER : look.header();
        var cells = new HashMap<Cell, ThemedCell>();

        var headerStyle = base.with(header.style());
        // The header look reaches across the table, whether the header is merged over it or not.
        for (var column = 0; column < table.getWidth(); column++) {
            cover(cells, table.getCell(column, 0), headerStyle);
        }
        // Only the cell holding the text formats it in pieces; the rest of the merged header carries none.
        var text = headerCell.getAbsoluteRegion();
        cells.put(new Cell(text.getTop(), text.getLeft()), new ThemedCell(headerStyle, header));

        var bodyStart = PropertiesHelper.getPropertiesTableSection(logical) == null ? 1 : 2;
        if (logical.getHeight() > bodyStart) {
            themeBody(cells, logical.getRows(bodyStart), isTransposed(kind), base, look, vocabulary);
        }
        // A region of empty cells does not widen the table, so it may be merged past the edge of the table: the cells
        // beyond the edge are not the table's.
        var region = table.getRegion();
        cells.keySet().removeIf(cell -> !GridRegionUtils.contains(region, cell.column(), cell.row()));
        return new ThemedTable(Map.copyOf(cells));
    }

    private static void themeBody(Map<Cell, ThemedCell> cells, ILogicalTable body, boolean transposed,
                                  ThemeStyle base, TableTheme.Look look, boolean vocabulary) {
        // A transposed table keeps its fields in columns, so its places are found with its rows and columns swapped.
        var columns = vocabulary ? null : DatatypeLayout.of(transposed ? body.transpose() : body);
        var last = body.getHeight() - 1;
        // A body has a handful of places, so each look is laid over the base once rather than once per cell.
        var looks = new HashMap<@Nullable ThemeStyle, ThemeStyle>();
        var lastLooks = new HashMap<ThemeStyle, ThemeStyle>();
        for (var row = 0; row <= last; row++) {
            for (var column = 0; column < body.getWidth(); column++) {
                var place = transposed ? placeOf(look, columns, column, row) : placeOf(look, columns, row, column);
                var style = looks.computeIfAbsent(place, base::with);
                cover(cells, body.getCell(column, row),
                        row == last ? lastLooks.computeIfAbsent(style, inRow -> inRow.with(look.lastRow())) : style);
            }
        }
    }

    /**
     * Whether a Datatype is written transposed, with a field in each column.
     *
     * <p>The compiler decides it from the types and the titles the table holds, so only a compiled table can be
     * transposed.
     */
    private static boolean isTransposed(IOpenLTable table) {
        return table.getSyntaxNode().getMetaInfoReader() instanceof DatatypeTableMetaInfoReader reader
                && reader.getBoundNode().getTable() instanceof ILogicalTable fields
                && !fields.isNormalOrientation();
    }

    /**
     * The look of a place in the body: the titles, the field types, the field names, or the other values.
     *
     * @param row    the row of the place in an upright table: the title row or a field
     * @param column the column of the place in an upright table
     */
    private static @Nullable ThemeStyle placeOf(TableTheme.Look look, DatatypeLayout.@Nullable Columns columns,
                                                int row, int column) {
        if (columns == null) {
            return look.values();
        }
        if (row < columns.firstFieldRow()) {
            return look.titles();
        }
        if (column == columns.type()) {
            return look.type();
        }
        return column == columns.name() ? look.name() : look.values();
    }

    /** Gives every cell of the region a cell covers the same look. */
    private static void cover(Map<Cell, ThemedCell> cells, ICell cell, ThemeStyle style) {
        var region = cell.getAbsoluteRegion();
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var column = region.getLeft(); column <= region.getRight(); column++) {
                cells.put(new Cell(row, column), new ThemedCell(style, null));
            }
        }
    }
}
