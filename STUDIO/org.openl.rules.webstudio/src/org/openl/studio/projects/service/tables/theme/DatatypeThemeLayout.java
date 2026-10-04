package org.openl.studio.projects.service.tables.theme;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.DatatypeLayout;
import org.openl.studio.projects.service.tables.theme.ThemedTable.Cell;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedCell;

/**
 * Decides which look a theme gives each cell of the body of a Datatype or a Vocabulary table.
 *
 * <p>The header and the properties are themed as every kind themes them, see {@link ThemeLayouts}. The rows below
 * them get the look of their column: the field types, the field names, or the other values. A Datatype that names
 * its columns gets the title look on that row. The last row gets the last-row look laid over its own. Every cell
 * starts from the look of the whole table.
 *
 * <p>A Datatype written transposed keeps a field in each column. It takes the looks the way it is compiled: the row
 * of field types takes the type look, the row of names takes the name look, and a column that names the rows takes
 * the title look. Its last row still closes the table. A table that did not compile is themed as it is written.
 */
final class DatatypeThemeLayout {

    private DatatypeThemeLayout() {
    }

    /**
     * Gives every cell of the body of a Datatype or a Vocabulary the look of its place.
     *
     * @param cells      the looks of the cells, which the body adds its cells to
     * @param body       the body of the table: its rows under the header and the properties
     * @param kind       the table, which tells whether it is compiled transposed
     * @param base       the look every cell of the table starts from
     * @param look       the look of the table
     * @param vocabulary whether the table is a Vocabulary
     */
    static void themeBody(Map<Cell, ThemedCell> cells, ILogicalTable body, IOpenLTable kind, ThemeStyle base,
                          TableTheme.Look look, boolean vocabulary) {
        var transposed = isTransposed(kind);
        // A transposed table keeps its fields in columns, so its places are found with its rows and columns swapped.
        var columns = vocabulary ? null : DatatypeLayout.of(transposed ? body.transpose() : body);
        var last = body.getHeight() - 1;
        for (var row = 0; row <= last; row++) {
            for (var column = 0; column < body.getWidth(); column++) {
                var place = transposed ? placeOf(look, columns, column, row) : placeOf(look, columns, row, column);
                var style = base.with(place);
                ThemeLayouts.cover(cells, body.getCell(column, row), row == last ? style.with(look.lastRow()) : style);
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
}
