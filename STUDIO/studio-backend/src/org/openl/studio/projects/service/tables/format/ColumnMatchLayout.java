package org.openl.studio.projects.service.tables.format;

import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.NAMES;
import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.VALUES;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.SubValue;
import org.openl.rules.cmatch.TableColumn;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.lang.xls.types.meta.ColumnMatchMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * Decides the role of each cell of the body of a ColumnMatch table, a decision tree that checks its arguments row by
 * row.
 *
 * <p>The first row of the body names the columns by their ids, such as {@code names}, {@code operation} or
 * {@code values}: code the compiler reads. The row under it titles what the table takes. The rows after them give
 * what the table returns or scores: their values are what it gives, and the rest of them titles of what it gives.
 * Every row after those is a condition. The name it checks is its key, and what it checks the name with and against
 * are values.
 *
 * <p>The algorithm the compiler read in the header tells how many rows give what the table returns or scores: three
 * for {@code WEIGHTED} — the return values, the total score and the score — and one for {@code MATCH}, the return
 * values, and for {@code SCORE}, the score. A header naming no algorithm is matched.
 *
 * <p>A condition whose name is not indented starts a group with the conditions indented under it, which the table
 * checks together. The first row of a group and the row after it start a group. The indent tells the compiler the
 * group, and formatting never changes it.
 *
 * <p>The ids, the rows and their indents are the ones the compiler read. A table the compiler read none of is read
 * from its text the same way: the ids in its first row, the algorithm its header names in angle brackets, and the
 * indents of the names of its conditions. An algorithm the compiler does not know is read as {@code MATCH}.
 */
final class ColumnMatchLayout {

    /** The row of the body the ids stand in. */
    private static final int IDS = 0;

    /** The row of the body the titles stand in, under the ids. */
    private static final int TITLES = 1;

    /** The values of a column a row has none in. */
    private static final SubValue[] NO_VALUES = {};

    private ColumnMatchLayout() {
    }

    /**
     * The places of the body of a ColumnMatch table the compiler read: its ids, its titles, the rows giving what it
     * returns or scores, and its conditions.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed layOut(TableBody body) {
        return body.compiled().node().getMetaInfoReader() instanceof ColumnMatchMetaInfoReader reader
                ? placed(body.rows(), Places.of(body, reader.getBoundNode().getColumnMatch()))
                : BodyLayout.Placed.plain(body);
    }

    /**
     * The places of the body of a ColumnMatch table the compiler read none of: its ids, the algorithm its header names
     * and the indents of its conditions, as written.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed guessed(TableBody body) {
        var algorithm = algorithmOf(GuessedReads.headerOf(body.compiled().node()));
        return placed(body.rows(), Places.guessed(body.rows(), algorithm));
    }

    /** The body with the parts of its places, and the rows of the sheet a group starts at. */
    private static BodyLayout.Placed placed(ILogicalTable rows, Places places) {
        var groupRows = places.groupEdges()
                .stream()
                .filter(row -> row < rows.getHeight())
                .map(row -> rows.getRow(row).getSource().getRegion().getTop())
                .collect(Collectors.toUnmodifiableSet());
        return new BodyLayout.Placed(rows, places, groupRows, Set.of());
    }

    /**
     * The algorithm the header of a table names in angle brackets right after its keyword, such as {@code <WEIGHTED>},
     * or {@code null} for a header naming none.
     */
    static @Nullable String algorithmOf(String header) {
        var open = header.indexOf('<');
        var close = open < 0 ? -1 : header.indexOf('>', open);
        if (close < 0) {
            return null;
        }
        var keyword = header.substring(0, open).strip();
        return keyword.isEmpty() || keyword.chars().anyMatch(Character::isWhitespace)
                ? null
                : header.substring(open + 1, close).trim();
    }

    /**
     * The parts of the places of one body.
     *
     * @param ids        the id of each column of the body the compiler read one for
     * @param special    the rows of the body that give what the table returns or scores
     * @param groupEdges the rows of the body a group starts at, and the rows after a group
     */
    private record Places(Map<Integer, String> ids,
                          Set<Integer> special,
                          Set<Integer> groupEdges) implements TableRegionLayout.PlaceRoles {

        private static final Place CODE = Place.of(RawTableRole.CODE);
        private static final Place TITLE = Place.of(RawTableRole.INPUT_TITLE);
        private static final Place RETURN_TITLE = Place.of(RawTableRole.OUTPUT_TITLE);
        private static final Place RETURNED = Place.of(RawTableRole.OUTPUT);
        private static final Place NAME = Place.of(RawTableRole.KEY);
        private static final Place VALUE = Place.of(RawTableRole.VALUE);

        static Places of(TableBody body, ColumnMatch columnMatch) {
            var compiledIds = columnMatch.getColumns().stream().map(TableColumn::getId).toList();
            var read = new Read(body, columnMatch.getRows(), compiledIds,
                    CompiledReads.specialRowsOf(columnMatch.getAlgorithm()));
            var ids = new HashMap<Integer, String>();
            compiledIds.forEach(id -> read.columnsOf(id).forEach(at -> ids.put(at, id)));
            return new Places(Map.copyOf(ids), read.specialRows(), read.groupEdges());
        }

        /**
         * The places of a body the compiler read none of: the ids written in its first row, the rows the algorithm
         * reads before the conditions, and the groups the indents of the names of the conditions make.
         */
        static Places guessed(ILogicalTable rows, @Nullable String algorithm) {
            var ids = new HashMap<Integer, String>();
            var written = TableRegionLayout.idsOf(rows);
            for (var column = 0; column < written.size(); column++) {
                if (!written.get(column).isEmpty()) {
                    ids.put(column, written.get(column));
                }
            }
            var count = CompiledReads.specialRowsOf(algorithm);
            if (count < 0) {
                count = CompiledReads.specialRowsOf((String) null);
            }
            var first = TITLES + 1;
            var conditions = Math.min(first + count, rows.getHeight());
            var names = ids.entrySet()
                    .stream()
                    .filter(id -> NAMES.equals(id.getValue()))
                    .mapToInt(Map.Entry::getKey)
                    .findFirst()
                    .orElse(0);
            var special = IntStream.range(first, conditions).boxed().collect(Collectors.toUnmodifiableSet());
            return new Places(Map.copyOf(ids), special, groupEdges(rows, names, conditions));
        }

        /**
         * The rows of a body a group of conditions starts at, and the rows after a group, by the indents written on
         * the sheet, as {@link Read#groupEdges} tells them of the rows the compiler read.
         */
        private static Set<Integer> groupEdges(ILogicalTable rows, int names, int from) {
            var edges = new HashSet<Integer>();
            var head = TableBody.NONE;
            for (var row = from; row <= rows.getHeight(); row++) {
                if (row == rows.getHeight() || !isIndented(rows.getCell(names, row))) {
                    if (head != TableBody.NONE && row > head + 1) {
                        edges.add(head);
                        edges.add(row);
                    }
                    head = row;
                }
            }
            return Set.copyOf(edges);
        }

        private static boolean isIndented(ICell cell) {
            return cell.getStyle() != null && cell.getStyle().getIndent() > 0;
        }

        /** What a cell of the body is, by the place it stands in. */
        @Override
        public Place at(ICell cell, int column, int row) {
            if (row == IDS) {
                return CODE;
            }
            if (row == TITLES) {
                return TITLE;
            }
            var id = ids.getOrDefault(column, "");
            if (special.contains(row)) {
                return VALUES.equals(id) ? RETURNED : RETURN_TITLE;
            }
            return NAMES.equals(id) ? NAME : VALUE;
        }
    }

    /**
     * The rows of a ColumnMatch table as the compiler read them, under its ids and its titles.
     *
     * @param body    the body of the table
     * @param rows    the rows the compiler read: the rows giving what the table returns or scores, then the conditions
     * @param ids     the id of each column the compiler read, in the order of the columns
     * @param special how many of the rows give what the table returns or scores
     */
    private record Read(TableBody body, List<TableRow> rows, List<String> ids, int special) {

        /** The columns of the body the values of a column the compiler named by an id stand in. */
        Set<Integer> columnsOf(String id) {
            var columns = new HashSet<Integer>();
            for (var row : rows) {
                for (var value : valuesOf(row, id)) {
                    columns.addAll(body.placesOf(value.getGridRegion(), true));
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
            var head = TableBody.NONE;
            for (var row = special; row <= rows.size(); row++) {
                if (row == rows.size() || !isIndented(row)) {
                    // Every row since the last condition that is not indented is indented under it.
                    if (head != TableBody.NONE && row > head + 1) {
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
         * The first row of the body a row the compiler read stands in: where its name stands, or where its first value
         * stands in a table that names no column {@code names}.
         */
        private Set<Integer> rowsOf(int row) {
            var compiled = rows.get(row);
            return Stream.concat(Stream.of(NAMES), ids.stream())
                    .map(id -> valuesOf(compiled, id))
                    .filter(values -> values.length > 0)
                    .findFirst()
                    .flatMap(values -> body.placesOf(values[0].getGridRegion(), false).stream()
                            .min(Integer::compareTo))
                    .map(Set::of)
                    .orElse(Set.of());
        }

        /** The row of the body after the one a row the compiler read stands in. */
        private Set<Integer> after(int row) {
            return rowsOf(row).stream().map(at -> at + 1).collect(Collectors.toUnmodifiableSet());
        }

        private static SubValue[] valuesOf(TableRow row, String id) {
            return Optional.ofNullable(row.get(id)).orElse(NO_VALUES);
        }
    }
}
