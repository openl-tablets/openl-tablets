package org.openl.studio.projects.service.tables.format;

import java.util.Set;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.lang.xls.types.meta.ConstantsTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * Decides the role of each cell of the body of a Datatype, a Vocabulary or a Constants table.
 *
 * <p>The header and the properties take the roles every kind gives them, see {@link TableRegionLayout}. The rows below
 * them take the role of their column: a type is code the compiler reads, a name is a key of the table, and every
 * other cell is a value. A Datatype that names its columns titles them on that row.
 *
 * <p>The columns are the ones the compiler read. A Datatype names them on a row of titles, or keeps the type, the name
 * and the default value of a field in its first three columns when it names none. A Constants table is read by place
 * alone: the type, the name and the value of each constant in its first three columns, under no titles. Every cell of
 * a Vocabulary is a value.
 *
 * <p>A Datatype written transposed keeps a field in each column, and a Constants table a constant. It takes the roles
 * the way it is compiled, so each place runs the other way.
 *
 * <p>A Datatype the compiler read none of, because its parent type is missing, is read from its text: its titles name
 * the columns when it has them, and else its first three columns are the type, the name and the default value of a
 * field. A Constants table the compiler read none of keeps them in the same columns. Whether such a table is written
 * transposed is told by its titles and its types, see {@link GuessedReads#typesTurned}.
 */
final class DatatypeLayout {

    /** The columns a Constants table, and a Datatype naming no columns, keep the type and the name in. */
    private static final int TYPE_COLUMN = 0;
    private static final int NAME_COLUMN = 1;

    private DatatypeLayout() {
    }

    /**
     * The places of the body of a Datatype: its fields, read the way the compiler read them. A transposed table keeps
     * its fields in columns.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed fields(TableBody body) {
        if (body.guessed()) {
            var upright = body.upright();
            var order = DatatypeHelper.getColumnTitlesOrder(upright);
            return new BodyLayout.Placed(upright, new Places(order.isEmpty() ? Set.of() : Set.of(0),
                    Set.of(order.getOrDefault(DatatypeHelper.TYPE_COLUMN_TITLE, TYPE_COLUMN)),
                    Set.of(order.getOrDefault(DatatypeHelper.NAME_COLUMN_TITLE, NAME_COLUMN))));
        }
        if (!(body.compiled().node().getMetaInfoReader() instanceof DatatypeTableMetaInfoReader reader)) {
            return BodyLayout.Placed.plain(body);
        }
        var datatype = reader.getBoundNode();
        var compiled = datatype.getTable();
        var order = datatype.getColumnTitlesOrder();
        return new BodyLayout.Placed(body.upright(), new Places(
                datatype.hasColumnTitles() ? body.rowsOf(compiled, 0) : Set.of(),
                body.columnsOf(compiled, order.get(DatatypeHelper.TYPE_COLUMN_TITLE)),
                body.columnsOf(compiled, order.get(DatatypeHelper.NAME_COLUMN_TITLE))));
    }

    /**
     * The places of the body of a Constants table: a constant in each row, or in each column of a transposed table,
     * its type, its name and its value by place.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed constants(TableBody body) {
        if (body.guessed()) {
            return new BodyLayout.Placed(body.upright(),
                    new Places(Set.of(), Set.of(TYPE_COLUMN), Set.of(NAME_COLUMN)));
        }
        if (!(body.compiled().node().getMetaInfoReader() instanceof ConstantsTableMetaInfoReader reader)) {
            return BodyLayout.Placed.plain(body);
        }
        var compiled = reader.getBoundNode().getNormalizedData();
        return new BodyLayout.Placed(body.upright(), new Places(Set.of(),
                body.columnsOf(compiled, TYPE_COLUMN),
                body.columnsOf(compiled, NAME_COLUMN)));
    }

    /**
     * The places of the body of a Vocabulary: its values.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed values(TableBody body) {
        return new BodyLayout.Placed(body.upright(), new Places(Set.of(), Set.of(), Set.of()));
    }

    /**
     * The parts of the places of one body.
     *
     * @param titleRows   the rows of the body that name the columns
     * @param typeColumns the columns of the body that hold the types
     * @param nameColumns the columns of the body that hold the names
     */
    private record Places(Set<Integer> titleRows, Set<Integer> typeColumns,
                          Set<Integer> nameColumns) implements TableRegionLayout.PlaceRoles {

        private static final Place TITLES = Place.of(RawTableRole.INPUT_TITLE);
        private static final Place TYPE = Place.of(RawTableRole.CODE);
        private static final Place NAME = Place.of(RawTableRole.KEY);
        private static final Place VALUES = Place.of(RawTableRole.VALUE);

        /**
         * What a place in the body is: a title of what the table takes, a type the compiler reads, the name of a
         * field or a constant, which is a key of the table, or a value.
         */
        @Override
        public Place at(ICell cell, int column, int row) {
            if (titleRows.contains(row)) {
                return TITLES;
            }
            if (typeColumns.contains(column)) {
                return TYPE;
            }
            return nameColumns.contains(column) ? NAME : VALUES;
        }
    }
}
