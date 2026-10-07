package org.openl.studio.projects.model.tables;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.table.ILogicalTable;
import org.openl.util.CollectionUtils;

/**
 * Where a datatype table keeps each of its columns.
 *
 * <p>A datatype keeps a field's type, name and default in the first three columns, and that layout needs no titles.
 * A field that also carries a mandatory flag, a description or an example cannot be laid out by position, so the
 * table names its columns on a title row above the fields, in any order it likes.
 *
 * <p>Everything that lays a datatype out asks here — the model for how much room a new table takes, the writer for
 * where to put each value, the reader for where to find it — so they cannot disagree.
 */
public final class DatatypeLayout {

    /** No column of a body keeps that value. */
    public static final int ABSENT = -1;

    private static final int TYPE_COLUMN = 0;
    private static final int NAME_COLUMN = 1;

    /** The type and the name, which every layout opens with. */
    private static final int LEADING_COLUMNS = 2;

    /** Where a table without titles keeps the default, right after the leading two. */
    private static final int DEFAULT_COLUMN = LEADING_COLUMNS;

    /** The three columns a table without titles has at most: the leading two and the default. */
    private static final int POSITIONAL_WIDTH = DEFAULT_COLUMN + 1;

    /**
     * A column a field may declare besides its type and name.
     *
     * @param title      the name the column carries on a title row
     * @param valueOf    the value a field keeps in it
     * @param into       puts a value read from the sheet back onto a field
     * @param typedValue whether the cell holds a value of the field's own type rather than plain text
     */
    public record OptionalColumn(String title,
                                 Function<DatatypeFieldView, Object> valueOf,
                                 BiConsumer<DatatypeFieldView.Builder, Object> into,
                                 boolean typedValue) {
    }

    /** Every optional column, in the order a new table lays them out. */
    public static final List<OptionalColumn> OPTIONAL_COLUMNS = List.of(
            new OptionalColumn(DatatypeHelper.DEFAULT_COLUMN_TITLE,
                    field -> field.defaultValue,
                    DatatypeFieldView.Builder::defaultValue,
                    true),
            new OptionalColumn(DatatypeHelper.MANDATORY_COLUMN_TITLE,
                    field -> field.mandatory,
                    (builder, value) -> builder.mandatory((String) value),
                    false),
            new OptionalColumn(DatatypeHelper.DESCRIPTION_COLUMN_TITLE,
                    field -> field.description,
                    (builder, value) -> builder.description((String) value),
                    false),
            new OptionalColumn(DatatypeHelper.EXAMPLE_COLUMN_TITLE,
                    field -> field.example,
                    DatatypeFieldView.Builder::example,
                    true));

    private DatatypeLayout() {
    }

    /**
     * Where a body that already exists keeps each column.
     *
     * <p>A titled body is read by its titles, so a reordered layout is found where it really is. Anything else is
     * the legacy positional layout, whose first row is already a field.
     *
     * @param body the table body below the header
     * @return the layout of that body
     */
    public static Columns of(ILogicalTable body) {
        var titles = DatatypeHelper.getColumnTitlesOrder(body);
        if (titles.isEmpty()) {
            return positional(body.getWidth());
        }
        var optional = new LinkedHashMap<String, Integer>();
        for (var column : OPTIONAL_COLUMNS) {
            var position = titles.get(column.title());
            if (position != null) {
                optional.put(column.title(), position);
            }
        }
        return new Columns(titles.get(DatatypeHelper.TYPE_COLUMN_TITLE),
                titles.get(DatatypeHelper.NAME_COLUMN_TITLE),
                optional,
                true);
    }

    /**
     * The layout a table created from these fields gets.
     *
     * <p>Fields that carry no more than a default keep the legacy three-column layout. Anything more has to name
     * its columns, because the extra ones cannot be told apart by position.
     *
     * @param fields the fields the table has to hold
     * @return the layout to write them in
     */
    public static Columns forFields(Collection<DatatypeFieldView> fields) {
        var declared = OPTIONAL_COLUMNS.stream()
                .filter(column -> declaredBy(fields, column))
                .map(OptionalColumn::title)
                .toList();
        if (declared.stream().allMatch(DatatypeHelper.DEFAULT_COLUMN_TITLE::equals)) {
            return positional(POSITIONAL_WIDTH);
        }
        var optional = new LinkedHashMap<String, Integer>();
        declared.forEach(title -> optional.put(title, LEADING_COLUMNS + optional.size()));
        return new Columns(TYPE_COLUMN, NAME_COLUMN, optional, true);
    }

    /**
     * How many rows a new table holding these fields takes up, the title row included.
     *
     * @param fields the fields the table has to hold
     * @return the number of rows, zero when there are no fields
     */
    public static int height(Collection<DatatypeFieldView> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return 0;
        }
        return fields.size() + forFields(fields).firstFieldRow();
    }

    /**
     * Whether the field puts a value in that column.
     *
     * <p>A blank counts as no value: a form that submits its empty inputs must not reshape the table around
     * columns that would stay empty, nor be refused for a value it does not really carry.
     *
     * @param field  the field to look at
     * @param column the column to look for
     * @return {@code true} when the field has something to keep in that column
     */
    public static boolean carries(DatatypeFieldView field, OptionalColumn column) {
        var value = column.valueOf().apply(field);
        return value != null && !(value instanceof String text && text.isBlank());
    }

    private static boolean declaredBy(Collection<DatatypeFieldView> fields, OptionalColumn column) {
        return CollectionUtils.isNotEmpty(fields) && fields.stream().anyMatch(field -> carries(field, column));
    }

    /**
     * The legacy layout, which has as many of its three columns as the body is wide — a body of two columns
     * declares no default, and writing one into it would widen the table.
     */
    private static Columns positional(int width) {
        var optional = width > DEFAULT_COLUMN
                ? Map.of(DatatypeHelper.DEFAULT_COLUMN_TITLE, DEFAULT_COLUMN)
                : Map.<String, Integer>of();
        return new Columns(TYPE_COLUMN, NAME_COLUMN, optional, false);
    }

    /**
     * Where one body keeps each column, and which row its fields start on.
     *
     * @param optional position of every column the body declares besides the type and the name
     * @param titled   whether the body names its columns on a row of its own
     */
    public record Columns(int type, int name, Map<String, Integer> optional, boolean titled) {

        /** Where the body keeps that column, or {@link #ABSENT} when it declares none. */
        public int at(String title) {
            return optional.getOrDefault(title, ABSENT);
        }

        /** The row the fields start on, right after the titles when there are any. */
        public int firstFieldRow() {
            return titled ? 1 : 0;
        }

        /** How many columns the body takes up: the type, the name, and everything it declares besides them. */
        public int width() {
            return LEADING_COLUMNS + optional.size();
        }
    }
}
