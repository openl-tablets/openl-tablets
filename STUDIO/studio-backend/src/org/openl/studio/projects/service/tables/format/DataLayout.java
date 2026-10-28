package org.openl.studio.projects.service.tables.format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.ITable;
import org.openl.rules.data.ITableModel;
import org.openl.rules.lang.xls.types.meta.DataTableMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * Decides the role of each cell of the body of a Data, a Test or a Run table.
 *
 * <p>The body names its fields before it holds any value, as the compiler reads it: a row of field names, a row of
 * the tables some fields take their values from when the table refers to any, and a row of titles. Each row under them
 * holds the values of one row of the table. The rows naming the fields are code the compiler reads. A Data table gives
 * its rows, so its titles are titles of what the table gives and its values are what it gives. In a Test and a Run
 * table the titles are titles of what the table takes, and every other value is a value the table takes.
 *
 * <p>The keys of the table are its keys, as the compiled table tells them. In a Data table they are the values of the
 * column a reference reads its rows by, unless the table names that column {@code _PK_}: such a column names the keys
 * itself and holds values. In a Test and a Run table they are the values of every column that takes them from a Data
 * table by their keys. A column of a Test table whose field starts with {@code _res_} or {@code _error_} holds the
 * result the test expects: its title is a title of what the table gives, and its values are what it gives.
 *
 * <p>A row of the table may take several lines: its key is merged over them, and the elements of a collection take a
 * line each. Once any row of the table takes several lines, a line above every row after the first one sets the rows
 * apart, as a group of rules is set apart in a decision table.
 *
 * <p>A table written transposed holds a field in each row and the values of one row in each column. It takes the
 * roles the way it is compiled, so each place runs the other way.
 *
 * <p>A table the compiler read none of, because its type or its tested table is missing, is read from its text the
 * same way. It is transposed when its field names stand down its first column, see {@link GuessedReads#fieldsTurned}.
 * Its keys are the ones the compiler would read: in a Data table the first field, unless it is {@code _PK_}, and in a
 * Test and a Run table every field that takes its values from a Data table by a {@code >} reference.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DataLayout {

    /** The row of field names, the first row of the body. */
    private static final int FIELD_NAMES = 0;

    /** The row of the tables the fields take their values from, when the body has one. */
    private static final int REFERENCES = 1;

    /**
     * The places of the body of a Data table, whose IDs name its rows.
     *
     * @param body the body of the table
     * @return the body, read the way the compiler reads it, and what each of its places is
     */
    static BodyLayout.Placed data(TableBody body) {
        return layOut(body, false, keyColumns(body, false));
    }

    /**
     * The places of the body of a Data table the compiler read none of, its keys as its text names them.
     *
     * @param body the body of the table
     * @return the body, read the way its text tells, and what each of its places is
     */
    static BodyLayout.Placed guessedData(TableBody body) {
        return layOut(body, false, guessedKeys(body.upright(), false));
    }

    /**
     * The places of the body of a Test or a Run table, each row of which calls a method with its values.
     *
     * @param body the body of the table
     * @return the body, read the way the compiler reads it, and what each of its places is
     */
    static BodyLayout.Placed calls(TableBody body) {
        return layOut(body, true, keyColumns(body, true));
    }

    /**
     * The places of the body of a Test or a Run table the compiler read none of, its keys as its text names them.
     *
     * @param body the body of the table
     * @return the body, read the way its text tells, and what each of its places is
     */
    static BodyLayout.Placed guessedCalls(TableBody body) {
        return layOut(body, true, guessedKeys(body.upright(), true));
    }

    /**
     * The places of a body read the way the compiler reads it: a field in each column, and the values of one row in
     * each row.
     */
    private static BodyLayout.Placed layOut(TableBody body, boolean calls, Set<Integer> keys) {
        var fields = body.upright();
        var titlesRow = hasReferences(fields) ? REFERENCES + 1 : REFERENCES;
        var places = new Places(titlesRow, keys, calls ? resultColumns(fields)::contains : column -> true);
        var groups = recordGroups(fields, titlesRow + 1, body.transposed());
        return body.transposed()
                ? new BodyLayout.Placed(fields, places, Set.of(), groups)
                : new BodyLayout.Placed(fields, places, groups, Set.of());
    }

    /**
     * The lines of the sheet that start a row of the table after its first one, where a row of the table takes
     * several lines: its key merged over them, the elements of a collection one per line. A table whose every row
     * takes one line has no groups. A line is a row of the sheet, or a column of it where the table is transposed.
     */
    private static Set<Integer> recordGroups(ILogicalTable fields, int firstRecord, boolean transposed) {
        var starts = new HashSet<Integer>();
        var severalLines = false;
        for (var record = firstRecord; record < fields.getHeight(); record++) {
            var region = fields.getRow(record).getCell(0, 0).getAbsoluteRegion();
            severalLines |= fields.getRow(record).getSource().getHeight() > 1;
            if (record > firstRecord) {
                starts.add(transposed ? region.getLeft() : region.getTop());
            }
        }
        return severalLines ? Set.copyOf(starts) : Set.of();
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

    /**
     * The columns of the body of a table the compiler read none of whose values are keys, as its text names them: the
     * first field of a Data table, unless it is {@code _PK_}, or the fields of a Test and a Run table that take their
     * values from a Data table by a {@code >} reference.
     */
    private static Set<Integer> guessedKeys(ILogicalTable fields, boolean calls) {
        if (calls) {
            return hasReferences(fields)
                    ? columns(fields, column -> GuessedReads.textAt(fields, column, REFERENCES).startsWith(">"))
                    : Set.of();
        }
        return fields.getWidth() > 0 && !DataTableBindHelper.FPK.equals(GuessedReads.textAt(fields, 0, FIELD_NAMES))
                ? Set.of(0)
                : Set.of();
    }

    /** Whether the body names the tables some fields take their values from, in the row under the field names. */
    private static boolean hasReferences(ILogicalTable fields) {
        return fields.getHeight() > REFERENCES && DataTableBindHelper.hasForeignKeysRow(fields);
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
            var field = GuessedReads.textAt(fields, column, FIELD_NAMES);
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

    /**
     * What the places of one body are.
     *
     * @param titlesRow     the row of titles, which follows the rows naming the fields
     * @param idColumns     the columns whose values are keys of a Data table
     * @param gives         whether a column holds what the table gives: every column of a Data table, and the results
     *                      a Test table expects
     */
    private record Places(int titlesRow, Set<Integer> idColumns, IntPredicate gives)
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
            var result = gives.test(column);
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
