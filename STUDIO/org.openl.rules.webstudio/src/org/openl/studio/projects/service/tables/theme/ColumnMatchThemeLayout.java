package org.openl.studio.projects.service.tables.theme;

import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.NAMES;
import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.VALUES;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;

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
 * <p>The algorithm the header names tells how many rows give what the table returns or scores: three for
 * {@code WEIGHTED} — the return values, the total score and the score — and one for {@code MATCH}, the return values,
 * and for {@code SCORE}, the score. A header naming no algorithm is matched.
 *
 * <p>A condition whose name is not indented starts a group with the conditions indented under it, which the table
 * checks together. The first row of a group and the row after it get the group look over their own. The indent tells
 * the compiler the group, and the theme never changes it.
 */
final class ColumnMatchThemeLayout {

    /** The row of the body the rows giving what the table returns or scores start at, under the ids and the titles. */
    private static final int RETURNS = 2;

    /** No row of the body. */
    private static final int NONE = -1;

    private ColumnMatchThemeLayout() {
    }

    /**
     * The places of the body of a ColumnMatch table, read as it is written. The header names the algorithm of the
     * table.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed layOut(ThemedBody body) {
        return new BodyLayout.Placed(body.rows(), Places.of(body));
    }

    /** An algorithm a ColumnMatch table is compiled by, by the name {@code MatchAlgorithmFactory} knows it by. */
    @RequiredArgsConstructor
    private enum Algorithm {
        MATCH(1),
        SCORE(1),
        WEIGHTED(3);

        /** The rows giving what the table returns or scores, as the compiler of the algorithm counts them. */
        private final int returns;

        /**
         * The algorithm a header names in angle brackets before the type it returns. A header naming none, or naming
         * one by another name, is matched.
         */
        static Algorithm of(String header) {
            var type = HeaderRuns.returnType(header);
            var end = type.indexOf('>');
            var name = type.startsWith("<") && end > 0 ? type.substring(1, end) : null;
            return EnumUtils.getEnum(Algorithm.class, name, MATCH);
        }
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
     * @param ids         the id of each column of the body
     * @param conditions  the row of the body the conditions start at
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
                          List<String> ids,
                          int conditions,
                          Set<Integer> groupEdges) implements ThemeLayouts.PlaceLook {

        static Places of(ThemedBody body) {
            var base = body.base();
            var look = body.look();
            var ids = ThemeLayouts.idsOf(body.rows());
            var conditions = RETURNS + Algorithm.of(body.header()).returns;
            return Places.builder()
                    .code(base.with(look.code()))
                    .title(base.with(look.titles()))
                    .returnTitle(base.with(look.returnTitles()))
                    .returned(base.with(look.returns()))
                    .name(base.with(look.name()))
                    .value(base.with(look.values()))
                    .groups(look.groups())
                    .ids(ids)
                    .conditions(conditions)
                    .groupEdges(groupEdgesOf(body.rows(), ids.indexOf(NAMES), conditions))
                    .build();
        }

        /** The look of a cell of the body, by the place it stands in. */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (row == 0) {
                return code;
            }
            if (row == 1) {
                return title;
            }
            var id = ids.get(column);
            if (row < conditions) {
                return VALUES.equals(id) ? returned : returnTitle;
            }
            var style = NAMES.equals(id) ? name : value;
            return groupEdges.contains(row) ? style.with(groups) : style;
        }
    }

    /**
     * The rows of the body a group of conditions starts at, and the rows after a group.
     *
     * <p>A group is a condition whose name is not indented, with the conditions indented under it. The row after the
     * last group is beyond the body when the group ends the table.
     *
     * @param body       the body of the table
     * @param names      the column of the names, or {@code -1} for a table that names none
     * @param conditions the row of the body the conditions start at
     * @return the rows of the body
     */
    private static Set<Integer> groupEdgesOf(ILogicalTable body, int names, int conditions) {
        var edges = new HashSet<Integer>();
        if (names < 0) {
            return edges;
        }
        var head = NONE;
        for (var row = conditions; row <= body.getHeight(); row++) {
            if (row == body.getHeight() || !isIndented(body.getCell(names, row))) {
                // Every row since the last condition that is not indented is indented under it.
                if (head != NONE && row > head + 1) {
                    edges.add(head);
                    edges.add(row);
                }
                head = row;
            }
        }
        return edges;
    }

    /** Whether the text of a cell is indented, which the compiler reads as a condition under the one above. */
    private static boolean isIndented(ICell cell) {
        var style = cell.getStyle();
        return style != null && style.getIndent() > 0;
    }
}
