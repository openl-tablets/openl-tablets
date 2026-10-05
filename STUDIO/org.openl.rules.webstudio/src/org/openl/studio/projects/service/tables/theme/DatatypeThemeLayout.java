package org.openl.studio.projects.service.tables.theme;

import java.util.Set;

import lombok.Builder;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.lang.xls.types.meta.ConstantsTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.table.ICell;

/**
 * Decides which look a theme gives each cell of the body of a Datatype, a Vocabulary or a Constants table.
 *
 * <p>The header and the properties are themed as every kind themes them, see {@link ThemeLayouts}. The rows below
 * them get the look of their column: the field types, the field names, or the other values. A Datatype that names
 * its columns gets the title look on that row. The last row gets the last-row look laid over its own. Every cell
 * starts from the look of the whole table.
 *
 * <p>The columns are the ones the compiler read. A Datatype names them on a row of titles, or keeps the type, the name
 * and the default value of a field in its first three columns when it names none. A Constants table is read by place
 * alone: the type, the name and the value of each constant in its first three columns, under no titles. Each column is
 * found where the edits since the compilation moved it. Every value of a Vocabulary gets the value look.
 *
 * <p>A Datatype written transposed keeps a field in each column, and a Constants table a constant. It takes the looks
 * the way it is compiled: the row of types takes the type look, the row of names takes the name look, and a column
 * that names the rows takes the title look. Its last row still closes the table. A table the compiler read none of
 * takes the look every cell starts from for its body, see {@link CompiledReads}.
 */
final class DatatypeThemeLayout {

    /** The columns a Constants table keeps the type and the name of a constant in. */
    private static final int CONSTANT_TYPE = 0;
    private static final int CONSTANT_NAME = 1;

    private DatatypeThemeLayout() {
    }

    /**
     * The places of the body of a Datatype: its fields, read the way the compiler read them. A transposed table keeps
     * its fields in columns.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed fields(ThemedBody body) {
        if (!(body.compiled().node().getMetaInfoReader() instanceof DatatypeTableMetaInfoReader reader)) {
            return BodyLayout.Placed.plain(body);
        }
        var datatype = reader.getBoundNode();
        var compiled = datatype.getTable();
        var order = datatype.getColumnTitlesOrder();
        return new BodyLayout.Placed(body.upright(), Places.of(body,
                datatype.hasColumnTitles() ? body.rowsNow(compiled, 0) : Set.of(),
                body.columnsNow(compiled, order.get(DatatypeHelper.TYPE_COLUMN_TITLE)),
                body.columnsNow(compiled, order.get(DatatypeHelper.NAME_COLUMN_TITLE))));
    }

    /**
     * The places of the body of a Constants table: a constant in each row, or in each column of a transposed table,
     * its type, its name and its value by place.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed constants(ThemedBody body) {
        if (!(body.compiled().node().getMetaInfoReader() instanceof ConstantsTableMetaInfoReader reader)) {
            return BodyLayout.Placed.plain(body);
        }
        var compiled = reader.getBoundNode().getNormalizedData();
        return new BodyLayout.Placed(body.upright(), Places.of(body, Set.of(),
                body.columnsNow(compiled, CONSTANT_TYPE),
                body.columnsNow(compiled, CONSTANT_NAME)));
    }

    /**
     * The places of the body of a Vocabulary: its values.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed values(ThemedBody body) {
        return new BodyLayout.Placed(body.upright(), Places.of(body, Set.of(), Set.of(), Set.of()));
    }

    /**
     * The looks of the places of one body, each laid over the base once.
     *
     * @param titleRows   the rows of the body that name the columns
     * @param typeColumns the columns of the body that hold the types
     * @param nameColumns the columns of the body that hold the names
     * @param titles      the look of the row that names the columns
     * @param type        the look of a type
     * @param name        the look of a name
     * @param values      the look of any other value
     */
    @Builder
    private record Places(Set<Integer> titleRows, Set<Integer> typeColumns, Set<Integer> nameColumns,
                          ThemeStyle titles, ThemeStyle type, ThemeStyle name,
                          ThemeStyle values) implements ThemeLayouts.PlaceLook {

        static Places of(ThemedBody body, Set<Integer> titleRows, Set<Integer> typeColumns,
                         Set<Integer> nameColumns) {
            var base = body.base();
            var look = body.look();
            return Places.builder()
                    .titleRows(titleRows)
                    .typeColumns(typeColumns)
                    .nameColumns(nameColumns)
                    .titles(base.with(look.titles()))
                    .type(base.with(look.type()))
                    .name(base.with(look.name()))
                    .values(base.with(look.values()))
                    .build();
        }

        /** The look of a place in the body: the titles, the types, the names, or the other values. */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (titleRows.contains(row)) {
                return titles;
            }
            if (typeColumns.contains(column)) {
                return type;
            }
            return nameColumns.contains(column) ? name : values;
        }
    }
}
