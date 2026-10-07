package org.openl.studio.projects.service.tables.theme;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.ITable;
import org.openl.rules.data.ITableModel;
import org.openl.rules.lang.xls.types.meta.DataTableMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.util.StringUtils;

/**
 * Decides which look a theme gives each cell of the body of a Data, a Test or a Run table.
 *
 * <p>The body names its fields before it holds any value, as the compiler reads it: a row of field names, a row of
 * the tables some fields take their values from when the table refers to any, and a row of titles. Each row under them
 * holds the values of one row of the table. The rows naming the fields get the name look, and a line it draws above or
 * below goes round these rows, not round each of them. The titles get the title look, and the values the value look.
 * Every cell starts from the look of the whole table, and a cell that reaches the bottom of the table gets the
 * last-row look over its own.
 *
 * <p>The keys of a Data table get the ID look over their own, as the compiled table tells them. In a Data table they
 * are the values of the column a reference reads its rows by, the key of the table, unless the table names that column
 * {@code _PK_}: such a column names the keys itself and takes no ID look. In a Test and a Run table they are the values
 * of every column that takes them from a Data table by their keys. A value that is not filled gets the empty look
 * over its own. A column of a Test table whose field starts with {@code _res_} or {@code _error_} holds the result the
 * test expects: its title gets the return title look over the title look, and its values the return look over the
 * value look.
 *
 * <p>A table written transposed holds a field in each row and the values of one row in each column. It takes the
 * looks the way it is compiled, so each place runs the other way. Its field names and its titles then label its rows:
 * they line up as the style every cell starts from does, and no line goes round the names. A table the compiler read
 * none of takes the look every cell starts from for its body, see {@link CompiledReads}.
 */
final class DataThemeLayout {

    /** The row of field names, the first row of the body. */
    private static final int FIELD_NAMES = 0;

    /** The row of the tables the fields take their values from, when the body has one. */
    private static final int REFERENCES = 1;

    private DataThemeLayout() {
    }

    /**
     * The places of the body of a Data table, whose IDs name its rows.
     *
     * @param body the body of the table
     * @return the body, read the way the compiler reads it, and the look of each of its places
     */
    static BodyLayout.Placed data(ThemedBody body) {
        return layOut(body, false);
    }

    /**
     * The places of the body of a Test or a Run table, each row of which calls a method with its values.
     *
     * @param body the body of the table
     * @return the body, read the way the compiler reads it, and the look of each of its places
     */
    static BodyLayout.Placed calls(ThemedBody body) {
        return layOut(body, true);
    }

    /**
     * The places of a body read the way the compiler reads it: a field in each column, and the values of one row in
     * each row.
     */
    private static BodyLayout.Placed layOut(ThemedBody body, boolean calls) {
        var fields = body.upright();
        var references = fields.getHeight() > REFERENCES && DataTableBindHelper.hasForeignKeysRow(fields);
        var base = body.base();
        var look = body.look();
        var titles = base.with(look.titles());
        var values = base.with(look.values());
        var places = Places.builder()
                .base(base)
                .name(label(base.with(look.name()), body))
                .titles(label(titles, body))
                .values(values)
                .returnTitles(label(titles.with(look.returnTitles()), body))
                .returns(values.with(look.returns()))
                .ids(look.ids())
                .empty(look.empty())
                .titlesRow(references ? REFERENCES + 1 : REFERENCES)
                .idColumns(keyColumns(body, calls))
                .resultColumns(calls ? resultColumns(fields) : Set.of())
                .transposed(body.transposed())
                .build();
        return new BodyLayout.Placed(fields, places);
    }

    /**
     * The look of a field name or a title. In a table written transposed it labels a row, so it lines up as the
     * style every cell starts from does.
     */
    private static ThemeStyle label(ThemeStyle style, ThemedBody body) {
        return body.transposed() ? style.withAlign(body.base().align()) : style;
    }

    /**
     * The columns of the body whose values are keys of a Data table, as the compiler reads the table. In a Data table
     * it is the column a reference reads its rows by, unless the table names it {@code _PK_}: such a column names the
     * keys itself. In a Test and a Run table they are the columns that take their values from a Data table by their
     * keys.
     */
    private static Set<Integer> keyColumns(ThemedBody body, boolean calls) {
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
     * The looks of the places of one body.
     *
     * @param base          the look every cell starts from
     * @param name          the look of the rows naming the fields and the tables they take their values from
     * @param titles        the look of a title
     * @param values        the look of a value
     * @param returnTitles  the look of the title of a result a Test table expects
     * @param returns       the look of a result a Test table expects
     * @param ids           the look laid over a value that is a key of a Data table
     * @param empty         the look laid over a value that is not filled
     * @param titlesRow     the row of titles, which follows the rows naming the fields
     * @param idColumns     the columns whose values are keys of a Data table
     * @param resultColumns the columns that hold the results a Test table expects
     * @param transposed    whether the table is written transposed, its field names in a column of their own
     */
    @Builder
    private record Places(ThemeStyle base,
                          ThemeStyle name,
                          ThemeStyle titles,
                          ThemeStyle values,
                          ThemeStyle returnTitles,
                          ThemeStyle returns,
                          @Nullable ThemeStyle ids,
                          @Nullable ThemeStyle empty,
                          int titlesRow,
                          Set<Integer> idColumns,
                          Set<Integer> resultColumns,
                          boolean transposed) implements ThemeLayouts.PlaceLook {

        /**
         * The look of a cell of the sheet a place takes. A row of values can take several rows of the sheet, and a
         * field several columns: a value is told empty cell by cell.
         */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (row < titlesRow) {
                // The rows naming the fields are one block: a line of the look goes round it, in an upright table.
                return name.atEdges(base, !transposed && row == FIELD_NAMES, !transposed && row == titlesRow - 1);
            }
            var result = resultColumns.contains(column);
            if (row == titlesRow) {
                return result ? returnTitles : titles;
            }
            var value = result ? returns : values;
            value = idColumns.contains(column) ? value.with(ids) : value;
            return StringUtils.isBlank(cell.getStringValue()) ? value.with(empty) : value;
        }
    }
}
