package org.openl.studio.projects.service.tables.theme;

import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.NAMES;
import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.VALUES;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.SubValue;
import org.openl.rules.cmatch.TableColumn;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.lang.xls.types.meta.ColumnMatchMetaInfoReader;
import org.openl.rules.table.ICell;

/**
 * Decides which look a theme gives each cell of the body of a ColumnMatch table: a decision tree that checks its
 * arguments row by row.
 *
 * <p>The first row of the body names the columns by their ids, such as {@code names}, {@code operation} or
 * {@code values}, and gets the code look. The row under it titles the columns and gets the title look. The rows after
 * them give what the table returns or scores: their values get the returns look and the rest of them the return title
 * look. Every row after those is a condition. The name it checks gets the name look, and what it checks the name with
 * and against gets the value look. The last row gets the last-row look over its own.
 *
 * <p>The algorithm the compiler read in the header tells how many rows give what the table returns or scores: three
 * for {@code WEIGHTED} — the return values, the total score and the score — and one for {@code MATCH}, the return
 * values, and for {@code SCORE}, the score. A header naming no algorithm is matched.
 *
 * <p>A condition whose name is not indented starts a group with the conditions indented under it, which the table
 * checks together. The first row of a group and the row after it get the group look over their own. The indent tells
 * the compiler the group, and the theme never changes it.
 *
 * <p>The ids, the rows and their indents are the ones the compiler read, each found where the edits since the
 * compilation moved it.
 */
final class ColumnMatchThemeLayout {

    /** The row of the body the ids stand in. */
    private static final int IDS = 0;

    /** The row of the body the titles stand in, under the ids. */
    private static final int TITLES = 1;

    /** The values of a column a row has none in. */
    private static final SubValue[] NO_VALUES = {};

    private ColumnMatchThemeLayout() {
    }

    /**
     * The places of the body of a ColumnMatch table the compiler read: its ids, its titles, the rows giving what it
     * returns or scores, and its conditions.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed layOut(ThemedBody body) {
        if (!(body.compiled().node().getMetaInfoReader() instanceof ColumnMatchMetaInfoReader reader)) {
            return BodyLayout.Placed.plain(body);
        }
        return new BodyLayout.Placed(body.rows(), Places.of(body, reader.getBoundNode().getColumnMatch()));
    }

    /**
     * The looks of the places of one body, each laid over the base once rather than once per cell.
     *
     * @param code        the look of an id
     * @param title       the look of a column title
     * @param returnTitle the look of a row giving what the table returns or scores, but for its values
     * @param returned    the look of a value the table returns or scores with
     * @param name        the look of a name the table checks
     * @param value       the look of what a condition checks its name with and against
     * @param groups      the look laid over the first row of a group and the row after it
     * @param ids         the id of each column of the body the compiler read one for
     * @param special     the rows of the body that give what the table returns or scores
     * @param groupEdges  the rows of the body a group starts at, and the rows after a group
     */
    @Builder
    private record Places(ThemeStyle code,
                          ThemeStyle title,
                          ThemeStyle returnTitle,
                          ThemeStyle returned,
                          ThemeStyle name,
                          ThemeStyle value,
                          @Nullable ThemeStyle groups,
                          Map<Integer, String> ids,
                          Set<Integer> special,
                          Set<Integer> groupEdges) implements ThemeLayouts.PlaceLook {

        static Places of(ThemedBody body, ColumnMatch columnMatch) {
            var base = body.base();
            var look = body.look();
            var compiledIds = columnMatch.getColumns().stream().map(TableColumn::getId).toList();
            var read = new Read(body, columnMatch.getRows(), compiledIds,
                    CompiledReads.specialRowsOf(columnMatch.getAlgorithm()));
            var ids = new HashMap<Integer, String>();
            compiledIds.forEach(id -> read.columnsOf(id).forEach(at -> ids.put(at, id)));
            return Places.builder()
                    .code(base.with(look.code()))
                    .title(base.with(look.titles()))
                    .returnTitle(base.with(look.returnTitles()))
                    .returned(base.with(look.returns()))
                    .name(base.with(look.name()))
                    .value(base.with(look.values()))
                    .groups(look.groups())
                    .ids(Map.copyOf(ids))
                    .special(read.specialRows())
                    .groupEdges(read.groupEdges())
                    .build();
        }

        /** The look of a cell of the body, by the place it stands in. */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (row == IDS) {
                return code;
            }
            if (row == TITLES) {
                return title;
            }
            var id = ids.getOrDefault(column, "");
            if (special.contains(row)) {
                return VALUES.equals(id) ? returned : returnTitle;
            }
            var style = NAMES.equals(id) ? name : value;
            return groupEdges.contains(row) ? style.with(groups) : style;
        }
    }

    /**
     * The rows of a ColumnMatch table as the compiler read them, under its ids and its titles, and where they stand
     * now.
     *
     * @param body    the body of the table
     * @param rows    the rows the compiler read: the rows giving what the table returns or scores, then the conditions
     * @param ids     the id of each column the compiler read, in the order of the columns
     * @param special how many of the rows give what the table returns or scores
     */
    private record Read(ThemedBody body, List<TableRow> rows, List<String> ids, int special) {

        /** The columns of the body the values of a column the compiler named by an id stand in now. */
        Set<Integer> columnsOf(String id) {
            var columns = new HashSet<Integer>();
            for (var row : rows) {
                for (var value : valuesOf(row, id)) {
                    columns.addAll(body.placesNow(value.getGridRegion(), true));
                }
            }
            return columns;
        }

        /** The rows of the body that give what the table returns or scores. */
        Set<Integer> specialRows() {
            var at = new HashSet<Integer>();
            for (var row = 0; row < Math.min(special, rows.size()); row++) {
                at.addAll(rowsOf(row));
            }
            return Set.copyOf(at);
        }

        /**
         * The rows of the body a group of conditions starts at, and the rows after a group.
         *
         * <p>A group is a condition whose name is not indented, with the conditions indented under it. The row after
         * the last group is beyond the body when the group ends the table.
         */
        Set<Integer> groupEdges() {
            var edges = new HashSet<Integer>();
            var head = ThemedBody.NONE;
            for (var row = special; row <= rows.size(); row++) {
                if (row == rows.size() || !isIndented(row)) {
                    // Every row since the last condition that is not indented is indented under it.
                    if (head != ThemedBody.NONE && row > head + 1) {
                        edges.addAll(rowsOf(head));
                        edges.addAll(row == rows.size() ? after(rows.size() - 1) : rowsOf(row));
                    }
                    head = row;
                }
            }
            return Set.copyOf(edges);
        }

        /** Whether the compiler read the name a condition checks indented, as a condition under the one above. */
        private boolean isIndented(int row) {
            var names = valuesOf(rows.get(row), NAMES);
            return names.length > 0 && names[0].getIndent() > 0;
        }

        /**
         * The first row of the body a row the compiler read stands in now: where its name stands, or where its first
         * value stands in a table that names no column {@code names}.
         */
        private Set<Integer> rowsOf(int row) {
            var compiled = rows.get(row);
            return Stream.concat(Stream.of(NAMES), ids.stream())
                    .map(id -> valuesOf(compiled, id))
                    .filter(values -> values.length > 0)
                    .findFirst()
                    .flatMap(values -> body.placesNow(values[0].getGridRegion(), false).stream()
                            .min(Integer::compareTo))
                    .map(Set::of)
                    .orElse(Set.of());
        }

        /** The row of the body after the one a row the compiler read stands in now. */
        private Set<Integer> after(int row) {
            return rowsOf(row).stream().map(at -> at + 1).collect(Collectors.toUnmodifiableSet());
        }

        private static SubValue[] valuesOf(TableRow row, String id) {
            return Optional.ofNullable(row.get(id)).orElse(NO_VALUES);
        }
    }
}
