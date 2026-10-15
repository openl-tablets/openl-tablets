package org.openl.studio.projects.service.tables.format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.ITable;
import org.openl.rules.data.ITableModel;
import org.openl.rules.lang.xls.types.meta.DataTableMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.util.StringUtils;

/**
 * Decides the role of each cell of the body of a Data, a Test or a Run table.
 *
 * <p>The body names its fields before it holds any value, as the compiler reads it: a row of field names, a row of
 * the tables some fields take their values from when the table refers to any, and a row of titles. Each row under them
 * holds the values of one row of the table. The rows naming the fields are code the compiler reads, the titles are
 * titles of what the table takes, and every other value is a value the table takes.
 *
 * <p>The keys of the table are its keys, as the compiled table tells them. In a Data table they are the values of the
 * column a reference reads its rows by, unless the table names that column {@code _PK_}: such a column names the keys
 * itself and holds values. In a Test and a Run table they are the values of every column that takes them from a Data
 * table by their keys. A column of a Test table whose field starts with {@code _res_} or {@code _error_} holds the
 * result the test expects: its title is a title of what the table gives, and its values are what it gives.
 *
 * <p>A table written transposed holds a field in each row and the values of one row in each column. It takes the
 * roles the way it is compiled, so each place runs the other way. A table the compiler read none of holds values in
 * its body, see {@link CompiledReads}.
 */
final class DataLayout {

    /** The row of field names, the first row of the body. */
    private static final int FIELD_NAMES = 0;

    /** The row of the tables the fields take their values from, when the body has one. */
    private static final int REFERENCES = 1;

    private DataLayout() {
    }

    /**
     * The places of the body of a Data table, whose IDs name its rows.
     *
     * @param body the body of the table
     * @return the body, read the way the compiler reads it, and what each of its places is
     */
    static BodyLayout.Placed data(TableBody body) {
        return layOut(body, false);
    }

    /**
     * The places of the body of a Test or a Run table, each row of which calls a method with its values.
     *
     * @param body the body of the table
     * @return the body, read the way the compiler reads it, and what each of its places is
     */
    static BodyLayout.Placed calls(TableBody body) {
        return layOut(body, true);
    }

    /**
     * The places of a body read the way the compiler reads it: a field in each column, and the values of one row in
     * each row.
     */
    private static BodyLayout.Placed layOut(TableBody body, boolean calls) {
        var fields = body.upright();
        var references = fields.getHeight() > REFERENCES && DataTableBindHelper.hasForeignKeysRow(fields);
        var places = new Places(references ? REFERENCES + 1 : REFERENCES, keyColumns(body, calls),
                calls ? resultColumns(fields) : Set.of());
        return new BodyLayout.Placed(fields, places);
    }

    /**
     * The columns of the body whose values are keys of a Data table, as the compiler reads the table. In a Data table
     * it is the column a reference reads its rows by, unless the table names it {@code _PK_}: such a column names the
     * keys itself. In a Test and a Run table they are the columns that take their values from a Data table by their
     * keys.
     */
    private static Set<Integer> keyColumns(TableBody body, boolean calls) {
        if (!(body.compiled().node().getMetaInfoReader() instanceof DataTableMetaInfoReader reader)
                || !(reader.getBoundNode().getTable() instanceof ITable table) || table.getData() == null) {
            return Set.of();
        }
        var model = table.getDataModel();
        // The first row the compiler read values or titles from.
        var first = table.getData().getRow(0);
        var columns = new HashSet<Integer>();
        (calls ? referencesOf(model) : keyOf(model))
                .filter(key -> key < first.getWidth())
                .forEach(key -> columns.addAll(body.placesOf(first.getColumn(key).getSource(), true)));
        return Set.copyOf(columns);
    }

    /** The column a reference reads the rows of a Data table by, unless the table names it {@code _PK_}. */
    private static IntStream keyOf(ITableModel model) {
        var key = model.getKeyColumnIndex();
        var descriptor = model.getDescriptor(key);
        return descriptor == null || descriptor.isPrimaryKey() ? IntStream.empty() : IntStream.of(key);
    }

    /** The columns of a Test or a Run table that take their values from a Data table by their keys. */
    private static IntStream referencesOf(ITableModel model) {
        return Arrays.stream(model.getDescriptors())
                .filter(ColumnDescriptor::isReference)
                .mapToInt(ColumnDescriptor::getColumnIdx);
    }

    /**
     * The columns of a Test table that hold the result it expects, as the compiler tells them: the field starts with
     * {@code _res_}, the value the tested method returns, or with {@code _error_}, the error it reports.
     */
    private static Set<Integer> resultColumns(ILogicalTable fields) {
        return columns(fields, column -> {
            var field = StringUtils.trimToEmpty(textOf(fields, column, FIELD_NAMES));
            return field.startsWith(TestMethodHelper.EXPECTED_RESULT_NAME)
                    || field.startsWith(TestMethodHelper.EXPECTED_ERROR);
        });
    }

    private static Set<Integer> columns(ILogicalTable fields, IntPredicate taken) {
        return IntStream.range(0, fields.getWidth())
                .filter(taken)
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
    }

    private static @Nullable String textOf(ILogicalTable fields, int column, int row) {
        return fields.getCell(column, row).getStringValue();
    }

    /**
     * What the places of one body are.
     *
     * @param titlesRow     the row of titles, which follows the rows naming the fields
     * @param idColumns     the columns whose values are keys of a Data table
     * @param resultColumns the columns that hold the results a Test table expects
     */
    private record Places(int titlesRow, Set<Integer> idColumns, Set<Integer> resultColumns)
            implements TableRegionLayout.PlaceRoles {

        private static final Place NAME = Place.of(RawTableRole.CODE);
        private static final Place TITLE = Place.of(RawTableRole.INPUT_TITLE);
        private static final Place RESULT_TITLE = Place.of(RawTableRole.OUTPUT_TITLE);
        private static final Place KEY = Place.of(RawTableRole.KEY);
        private static final Place VALUE = Place.of(RawTableRole.VALUE);
        private static final Place RESULT = Place.of(RawTableRole.OUTPUT);

        /** What a cell of the body is, by the place it stands in. */
        @Override
        public Place at(ICell cell, int column, int row) {
            if (row < titlesRow) {
                return NAME;
            }
            var result = resultColumns.contains(column);
            if (row == titlesRow) {
                return result ? RESULT_TITLE : TITLE;
            }
            if (idColumns.contains(column)) {
                return KEY;
            }
            return result ? RESULT : VALUE;
        }
    }
}
