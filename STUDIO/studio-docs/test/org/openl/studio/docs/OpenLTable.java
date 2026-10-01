package org.openl.studio.docs;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.studio.docs.CsvRecords.Cell;
import org.openl.studio.docs.CsvRecords.Row;

/**
 * The rules an {@code openl} code block follows to draw an OpenL table.
 *
 * <p>The first line is the header of the table, taken as it is written, commas included, and it spans the whole
 * width. The other lines are CSV records. A {@code ---} line ends the column headers.
 *
 * <p>A cell {@code <} joins the cell on its left and a cell {@code ^} joins the cell above, and every merged area is a
 * rectangle. A record shorter than the widest one is padded with empty cells.
 *
 * @author Yury Molchan
 */
final class OpenLTable {

    /** The line ending the column headers. */
    static final String SEPARATOR = "---";

    private static final String NO_RECTANGLE = "The merged cells do not form a rectangle.";
    private static final char VALUE = 'v';
    private static final char LEFT = '<';
    private static final char UP = '^';

    private OpenLTable() {
    }

    /** What breaks the table an {@code openl} code block draws. */
    static List<Issue> issues(String text) {
        var lines = text.split("\n", -1);
        var header = IntStream.range(0, lines.length).filter(i -> !lines[i].isBlank()).findFirst().orElse(-1);
        if (header < 0) {
            return List.of(new Issue(0, "The table has no rows."));
        }
        var issues = new LinkedHashSet<Issue>();
        if (lines[header].strip().equals(SEPARATOR)) {
            issues.add(new Issue(header, "The table starts with `---`. Its first line is the table header."));
        }
        lines[header] = "";
        var records = CsvRecords.parse(String.join("\n", lines));
        issues.addAll(records.issues());
        var separators = records.rows().stream().filter(OpenLTable::isSeparator).toList();
        if (separators.size() > 1) {
            issues.add(new Issue(separators.get(1).line(), "A second `---` line. One line ends the column headers."));
        }
        mergeIssues(header, records.rows().stream().filter(row -> !isSeparator(row)).toList(), issues);
        return List.copyOf(issues);
    }

    private static boolean isSeparator(Row row) {
        return row.cells().size() == 1 && row.cells().getFirst().is(SEPARATOR);
    }

    /** Checks the merges of the rows under the header, which is a value spanning the whole width. */
    private static void mergeIssues(int header, List<Row> rows, LinkedHashSet<Issue> issues) {
        var kinds = kinds(rows);
        var lines = IntStream.concat(IntStream.of(header), rows.stream().mapToInt(Row::line)).toArray();
        var claimed = new boolean[kinds.length][kinds[0].length];
        for (var r = 0; r < kinds.length; r++) {
            for (var c = 0; c < kinds[r].length; c++) {
                var broken = kinds[r][c] == VALUE ? brokenRow(kinds, claimed, r, c) : -1;
                var marker = markerIssue(kinds[r][c], c);
                if (broken >= 0) {
                    issues.add(new Issue(lines[broken], NO_RECTANGLE));
                } else if (marker != null) {
                    issues.add(new Issue(lines[r], marker));
                }
            }
        }
        for (var r = 0; r < kinds.length; r++) {
            for (var c = 0; c < kinds[r].length; c++) {
                if (kinds[r][c] != VALUE && !claimed[r][c] && markerIssue(kinds[r][c], c) == null) {
                    issues.add(new Issue(lines[r], NO_RECTANGLE));
                }
            }
        }
    }

    /** The kind of every cell, the header first: a value or a merge marker. */
    private static char[][] kinds(List<Row> rows) {
        var width = rows.stream().mapToInt(row -> row.cells().size()).max().orElse(1);
        var kinds = new char[rows.size() + 1][width];
        Arrays.fill(kinds[0], LEFT);
        kinds[0][0] = VALUE;
        for (var r = 0; r < rows.size(); r++) {
            Arrays.fill(kinds[r + 1], VALUE);
            var cells = rows.get(r).cells();
            for (var c = 0; c < cells.size(); c++) {
                kinds[r + 1][c] = kind(cells.get(c));
            }
        }
        return kinds;
    }

    private static char kind(Cell cell) {
        if (cell.is("<")) {
            return LEFT;
        }
        return cell.is("^") ? UP : VALUE;
    }

    private static @Nullable String markerIssue(char kind, int column) {
        return kind == LEFT && column == 0 ? "A `<` in the first column has no cell on its left to join." : null;
    }

    /**
     * Claims the cells merged into the value at the given place.
     *
     * @return the row of the first cell breaking the rectangle the merged cells form, or {@code -1} when none does
     */
    private static int brokenRow(char[][] kinds, boolean[][] claimed, int row, int column) {
        var width = 1;
        while (column + width < kinds[row].length && kinds[row][column + width] == LEFT) {
            width++;
        }
        var height = 1;
        while (row + height < kinds.length && kinds[row + height][column] == UP) {
            height++;
        }
        var broken = -1;
        for (var r = row; r < row + height; r++) {
            for (var c = column; c < column + width; c++) {
                var merged = r != row || c != column;
                if (merged && (kinds[r][c] == VALUE || claimed[r][c])) {
                    broken = broken < 0 ? r : broken;
                } else if (merged) {
                    claimed[r][c] = true;
                }
            }
        }
        return broken;
    }
}
