package org.openl.studio.projects.service.tables.theme;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.ICell;
import org.openl.studio.projects.model.tables.DatatypeLayout;

/**
 * Decides which look a theme gives each cell of the body of a Datatype, a Vocabulary or a Constants table.
 *
 * <p>The header and the properties are themed as every kind themes them, see {@link ThemeLayouts}. The rows below
 * them get the look of their column: the field types, the field names, or the other values. A Datatype that names
 * its columns gets the title look on that row. The last row gets the last-row look laid over its own. Every cell
 * starts from the look of the whole table. A Constants table names its constants as a Datatype names its fields: the
 * type, the name and the value of each, which get the type, the name and the value look.
 *
 * <p>A Datatype written transposed keeps a field in each column, and a Constants table a constant. It takes the looks
 * the way it is compiled: the row of types takes the type look, the row of names takes the name look, and a column
 * that names the rows takes the title look. Its last row still closes the table. A table that did not compile is
 * themed as it is written.
 */
final class DatatypeThemeLayout {

    private DatatypeThemeLayout() {
    }

    /**
     * The places of the body of a Datatype or a Constants table: its fields, or its constants, read the way the
     * compiler reads them. A transposed table keeps its fields in columns.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed fields(ThemedBody body) {
        var fields = body.upright();
        return new BodyLayout.Placed(fields, Places.of(body, DatatypeLayout.of(fields)));
    }

    /**
     * The places of the body of a Vocabulary: one column of values.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed values(ThemedBody body) {
        return new BodyLayout.Placed(body.upright(), Places.of(body, null));
    }

    /**
     * The looks of the places of one body, each laid over the base once.
     *
     * @param columns the columns of a Datatype as an upright table holds them, or {@code null} for a Vocabulary
     * @param titles  the look of the row that names the columns
     * @param type    the look of a field type
     * @param name    the look of a field name
     * @param values  the look of any other value
     */
    @Builder
    private record Places(DatatypeLayout.@Nullable Columns columns, ThemeStyle titles, ThemeStyle type,
                          ThemeStyle name, ThemeStyle values) implements ThemeLayouts.PlaceLook {

        static Places of(ThemedBody body, DatatypeLayout.@Nullable Columns columns) {
            var base = body.base();
            var look = body.look();
            return Places.builder()
                    .columns(columns)
                    .titles(base.with(look.titles()))
                    .type(base.with(look.type()))
                    .name(base.with(look.name()))
                    .values(base.with(look.values()))
                    .build();
        }

        /** The look of a place in the body: the titles, the field types, the field names, or the other values. */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (columns == null) {
                return values;
            }
            if (row < columns.firstFieldRow()) {
                return titles;
            }
            if (column == columns.type()) {
                return type;
            }
            return column == columns.name() ? name : values;
        }
    }
}
