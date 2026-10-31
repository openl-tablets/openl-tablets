package org.openl.excel.parser.markdown;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

/**
 * Lays the rows of cells of a table out in its body, and back: a merged cell takes the rows and the columns it spans,
 * whatever syntax writes the table.
 *
 * <p>A cell takes the first column of its row that no cell above spans down into. The first row gives the width of
 * the table: a cell beyond it, and the part of a merged cell past the table or over another merged cell, is a problem,
 * and the table is read without it.
 */
final class TableLayout {

    private final List<TableRow> rows;
    private final int width;
    private final List<List<@Nullable String>> body;
    /** The columns merged cells take in the rows they span down into, for the rows not laid out yet. */
    private final Map<Integer, BitSet> taken = new HashMap<>();
    private final BitSet none = new BitSet(0);
    private final List<CellRangeAddress> merges = new ArrayList<>();
    private final List<Problem> problems = new ArrayList<>();

    /**
     * The body a table is laid out in.
     *
     * @param body     the rows of the body, as wide as its first row; a blank cell, and a cell a merged cell covers,
     *                 is {@code null}
     * @param merges   the merged cells
     * @param problems the problems the table is read without
     */
    record Body(List<List<@Nullable String>> body, List<CellRangeAddress> merges, List<Problem> problems) {
    }

    /**
     * A problem of the table.
     *
     * @param line    the line of the file, counted from zero
     * @param problem what is wrong
     * @param cell    the cell of the body the problem shows in
     */
    record Problem(int line, String problem, CellAddress cell) {
    }

    private TableLayout(List<TableRow> rows, int width) {
        this.rows = rows;
        this.width = width;
        this.body = new ArrayList<>(rows.size());
    }

    /**
     * Lays rows of cells out in the body of a table.
     */
    static Body bodyOf(List<TableRow> rows) {
        if (rows.isEmpty()) {
            return new Body(List.of(), List.of(), List.of());
        }
        var first = rows.getFirst();
        return new TableLayout(rows, first.cells().stream().mapToInt(TableCell::colspan).sum()).laidOut(first);
    }

    private Body laidOut(TableRow first) {
        for (var r = 0; r < rows.size(); r++) {
            var row = r == 0 ? first : rows.get(r);
            var cells = new ArrayList<@Nullable String>(Collections.nCopies(width, null));
            body.add(cells);
            var inRow = taken.getOrDefault(r, none);
            var column = inRow.nextClearBit(0);
            for (var cell : row.cells()) {
                if (column >= width) {
                    problem(row, "The row has more cells than the first row of the table", r, width - 1);
                    break;
                }
                var range = spanOf(row, cell, r, column);
                cells.set(column, cell.text());
                for (var below = range.getFirstRow() + 1; below <= range.getLastRow(); below++) {
                    taken.computeIfAbsent(below, b -> new BitSet(width))
                            .set(range.getFirstColumn(), range.getLastColumn() + 1);
                }
                if (range.getNumberOfCells() > 1) {
                    merges.add(range);
                }
                column = inRow.nextClearBit(range.getLastColumn() + 1);
            }
            taken.remove(r);
        }
        return new Body(body, merges, problems);
    }

    /**
     * Returns the cells a cell takes at a place of the body: its spans cut to the table, and to the cells no other
     * merged cell takes.
     */
    private CellRangeAddress spanOf(TableRow row, TableCell cell, int top, int left) {
        var bottom = Math.min(top + cell.rowspan() - 1, rows.size() - 1);
        var right = Math.min(left + cell.colspan() - 1, width - 1);
        if (bottom < top + cell.rowspan() - 1 || right < left + cell.colspan() - 1) {
            problem(row, "The merged cell reaches past the table", top, left);
        }
        // The cell itself is free, so a cut leaves it at least
        var cut = false;
        var inRow = takenIn(top, left, right);
        if (inRow >= 0) {
            right = inRow - 1;
            cut = true;
        }
        for (var r = top + 1; r <= bottom; r++) {
            if (takenIn(r, left, right) >= 0) {
                bottom = r - 1;
                cut = true;
            }
        }
        if (cut) {
            problem(row, "The merged cell overlaps another one", top, left);
        }
        return new CellRangeAddress(top, bottom, left, right);
    }

    /** Returns the first column between two that a merged cell takes in a row, or -1 when none is taken. */
    private int takenIn(int row, int left, int right) {
        var columns = taken.get(row);
        var next = columns == null ? -1 : columns.nextSetBit(left);
        return next >= 0 && next <= right ? next : -1;
    }

    private void problem(TableRow row, String problem, int r, int column) {
        problems.add(new Problem(row.lines().firstLine(), problem, new CellAddress(r, column)));
    }

    /**
     * Returns the cells of each row of a table as a file writes them: a merged cell with the rows and the columns it
     * spans, and none of the cells it covers. The cells of a row are made when the row is asked for.
     */
    static List<List<TableCell>> rowsOf(RulesTable table) {
        var cells = new RowCells(table);
        return new AbstractList<>() {
            @Override
            public List<TableCell> get(int r) {
                return cells.of(r);
            }

            @Override
            public int size() {
                return cells.body.size();
            }
        };
    }

    /**
     * Returns what tells each row of a table from another as a file writes it: two rows are written alike when their
     * keys are equal. A row no merged cell reaches is known by its values, so it is compared with no cell made; any
     * other by its cells, a merged cell with its spans.
     */
    static List<Object> keysOf(RulesTable table) {
        var cells = new RowCells(table);
        return new AbstractList<>() {
            @Override
            public Object get(int r) {
                var values = cells.body.get(r);
                return cells.covered.containsKey(r) || values.size() != cells.width ? cells.of(r) : values;
            }

            @Override
            public int size() {
                return cells.body.size();
            }
        };
    }

    /**
     * The merged cells of a table by the rows they reach, to make the cells of a row.
     */
    private static final class RowCells {

        private static final BitSet NONE = new BitSet(0);

        private final List<List<@Nullable String>> body;
        private final int width;
        /** The columns merged cells cover in each row they reach; the row a merged cell starts in is reached too. */
        private final Map<Integer, BitSet> covered = new HashMap<>();
        /** The merged cells of each row they start in, by their column. */
        private final Map<Integer, Map<Integer, CellRangeAddress>> spans = new HashMap<>();

        RowCells(RulesTable table) {
            body = table.body();
            width = table.width();
            for (var range : table.merges()) {
                spans.computeIfAbsent(range.getFirstRow(), row -> new HashMap<>()).put(range.getFirstColumn(), range);
                for (var r = range.getFirstRow(); r <= range.getLastRow(); r++) {
                    var columns = covered.computeIfAbsent(r, row -> new BitSet(width));
                    columns.set(range.getFirstColumn(), range.getLastColumn() + 1);
                    if (r == range.getFirstRow()) {
                        columns.clear(range.getFirstColumn());
                    }
                }
            }
        }

        List<TableCell> of(int r) {
            var values = body.get(r);
            var cells = new ArrayList<TableCell>(width);
            var skipped = covered.getOrDefault(r, NONE);
            var starting = spans.getOrDefault(r, Map.of());
            for (var c = skipped.nextClearBit(0); c < width; c = skipped.nextClearBit(c + 1)) {
                var range = starting.isEmpty() ? null : starting.get(c);
                var value = c < values.size() ? values.get(c) : null;
                cells.add(range == null ? TableCell.of(value) : new TableCell(value, range.getLastRow() - r + 1,
                        range.getLastColumn() - c + 1));
            }
            return cells;
        }
    }
}
