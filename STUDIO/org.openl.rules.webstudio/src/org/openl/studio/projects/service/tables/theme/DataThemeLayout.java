package org.openl.studio.projects.service.tables.theme;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.util.StringUtils;

/**
 * Decides which look a theme gives each cell of the body of a Data, a Test or a Run table.
 *
 * <p>The body names its fields before it holds any value, as the compiler reads it: a row of field names, a row of
 * the tables some fields take their values from when the table refers to any, and a row of titles. Each row under them
 * holds the values of one row of the table. The rows naming the fields get the name look, the titles the title look,
 * and the values the value look. Every cell starts from the look of the whole table, and a cell that reaches the
 * bottom of the table gets the last-row look over its own.
 *
 * <p>The values that name a row of a Data table get the ID look over their own. In a Data table these are its IDs: the
 * column {@code _PK_}, or else its first column. In a Test and a Run table they are the values of every column that
 * takes them from a Data table by their IDs. A value that is not filled gets the empty look over its own.
 *
 * <p>A table written transposed holds a field in each row and the values of one row in each column. It takes the
 * looks the way it is compiled, so each place runs the other way. A table that did not compile is themed as it is
 * written.
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
        var places = Places.builder()
                .name(base.with(look.name()))
                .titles(base.with(look.titles()))
                .values(base.with(look.values()))
                .ids(look.ids())
                .empty(look.empty())
                .titlesRow(references ? REFERENCES + 1 : REFERENCES)
                .idColumns(calls ? referenceColumns(fields, references) : keyColumns(fields))
                .build();
        return new BodyLayout.Placed(fields, places);
    }

    /** The column of the IDs of a Data table: the column {@code _PK_}, or else its first column. */
    private static Set<Integer> keyColumns(ILogicalTable fields) {
        return Set.of(IntStream.range(0, fields.getWidth())
                .filter(column -> DataTableBindHelper.FPK.equals(StringUtils.trim(textOf(fields, column, FIELD_NAMES))))
                .findFirst()
                .orElse(0));
    }

    /** The columns of a Test or a Run table that take their values from a Data table by their IDs. */
    private static Set<Integer> referenceColumns(ILogicalTable fields, boolean references) {
        if (!references) {
            return Set.of();
        }
        return IntStream.range(0, fields.getWidth())
                .filter(column -> !StringUtils.isBlank(textOf(fields, column, REFERENCES)))
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
    }

    private static @Nullable String textOf(ILogicalTable fields, int column, int row) {
        return fields.getCell(column, row).getStringValue();
    }

    /**
     * The looks of the places of one body.
     *
     * @param name      the look of the rows naming the fields and the tables they take their values from
     * @param titles    the look of the row of titles
     * @param values    the look of a value
     * @param ids       the look laid over a value that names a row of a Data table
     * @param empty     the look laid over a value that is not filled
     * @param titlesRow the row of titles, which follows the rows naming the fields
     * @param idColumns the columns whose values name a row of a Data table
     */
    @Builder
    private record Places(ThemeStyle name,
                          ThemeStyle titles,
                          ThemeStyle values,
                          @Nullable ThemeStyle ids,
                          @Nullable ThemeStyle empty,
                          int titlesRow,
                          Set<Integer> idColumns) implements ThemeLayouts.PlaceLook {

        /**
         * The look of a cell of the sheet a place takes. A row of values can take several rows of the sheet, and a
         * field several columns: a value is told empty cell by cell.
         */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (row < titlesRow) {
                return name;
            }
            if (row == titlesRow) {
                return titles;
            }
            var value = idColumns.contains(column) ? values.with(ids) : values;
            return StringUtils.isBlank(cell.getStringValue()) ? value.with(empty) : value;
        }
    }
}
