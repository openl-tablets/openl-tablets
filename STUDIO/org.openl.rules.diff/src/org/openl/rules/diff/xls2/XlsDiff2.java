package org.openl.rules.diff.xls2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.Getter;

import org.openl.OpenClassUtil;
import org.openl.classloader.OpenLClassLoader;
import org.openl.impl.DefaultCompileContext;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjectionDiffer;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.xls.Parser;

/**
 * Find difference between two XLS files. It compares per Table.
 * <p>
 * The cells of two versions of a table are compared row by row when they keep their width, and column by column
 * when they keep their height. A table that changed both is aligned first: its columns are paired by the values
 * they share, and its rows are then compared over the paired columns.
 *
 * @author Aleh Bykhavets
 */
public class XlsDiff2 {
    private List<XlsTable> tables1;
    private List<XlsTable> tables2;

    // Same Sheet, Location (Start, End), Header
    private static final String GUESS_SAME = "1-same";
    // Same Sheet, Location (Start, End)
    private static final String GUESS_SAME_PLACE = "2-samePlace";
    // Same Sheet, Start, Header
    private static final String GUESS_CAN_BE_SAME = "3-canBeSame";
    // Same Sheet, Header
    private static final String GUESS_MAY_BE_SAME = "4-mayBeSame";

    private final Map<String, List<DiffPair>> diffGuess;

    public XlsDiff2() {
        // TreeMap -- Key as a weight
        diffGuess = new TreeMap<>();
    }

    // OpenClassUtil releases the class loader and also clears the OpenL caches bound to it.
    @SuppressWarnings("java:S2093")
    private List<XlsTable> load(IOpenSourceCodeModule src) {
        final var oldCl = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = null;
        try {
            classLoader = new OpenLClassLoader(oldCl);
            Thread.currentThread().setContextClassLoader(classLoader);

            var pc = new Parser().parseAsModule(src);
            var bc = new XlsBinder(new DefaultCompileContext()).bind(pc);
            var ioc = bc.getTopNode().getType();

            var xmi = (XlsMetaInfo) ioc.getMetaInfo();
            var xsn = xmi.getXlsModuleNode();

            var nodes = xsn.getXlsTableSyntaxNodes();
            return Arrays.stream(nodes).map(XlsTable::new).collect(Collectors.toCollection(ArrayList::new));
        } finally {
            OpenClassUtil.releaseClassLoader(classLoader);
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public DiffTreeNode diffFiles(File xlsFile1, File xlsFile2) {
        load(xlsFile1, xlsFile2);
        diff();
        return buildTree();
    }

    private void load(File xlsFile1, File xlsFile2) {
        if (xlsFile1 == null) {
            tables1 = List.of();
        } else {
            tables1 = load(new URLSourceCodeModule(URLSourceCodeModule.toUrl(xlsFile1)));
        }
        if (xlsFile2 == null) {
            tables2 = List.of();
        } else {
            tables2 = load(new URLSourceCodeModule(URLSourceCodeModule.toUrl(xlsFile2)));
        }
    }

    private void add(String guess, DiffPair r) {
        var list = diffGuess.computeIfAbsent(guess, e -> new ArrayList<>());
        list.add(r);
    }

    private void diff() {
        // 1. Simple cases
        iterate(this::guessBySameStart);

        // 2. Sheet and name seems the same
        iterate((t1, t2) -> {
            if (Objects.equals(t1.getSheetName(), t2.getSheetName())) {
                var sameName = t1.getTableName().equals(t2.getTableName());
                if (sameName) {
                    add(GUESS_MAY_BE_SAME, new DiffPair(t1, t2));
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Pairs the tables starting at the same cell of the same sheet when they also end at the same cell or have the
     * same name.
     *
     * @return {@code true} if the tables are paired
     */
    private boolean guessBySameStart(XlsTable t1, XlsTable t2) {
        if (Objects.equals(t1.getSheetName(), t2.getSheetName())) {
            var s1 = t1.getLocation().getStart().toString();
            var s2 = t2.getLocation().getStart().toString();
            if (s1.equals(s2)) {
                var sameName = t1.getTableName().equals(t2.getTableName());

                var e1 = t1.getLocation().getEnd().toString();
                var e2 = t2.getLocation().getEnd().toString();
                if (e1.equals(e2)) {
                    if (sameName) {
                        add(GUESS_SAME, new DiffPair(t1, t2));
                    } else {
                        add(GUESS_SAME_PLACE, new DiffPair(t1, t2));
                    }
                    return true;
                } else if (sameName) {
                    add(GUESS_CAN_BE_SAME, new DiffPair(t1, t2));
                    return true;
                }
            }
        }
        return false;
    }

    private void iterate(IterClosure closure) {
        Iterator<XlsTable> i1 = tables1.iterator();
        while (i1.hasNext()) {
            var t1 = i1.next();

            Iterator<XlsTable> i2 = tables2.iterator();
            while (i2.hasNext()) {
                var t2 = i2.next();

                if (closure.remove(t1, t2)) {
                    i1.remove();
                    i2.remove();
                    break;
                }
            }
        }
    }

    private DiffTreeNode buildTree() {
        var builder = new DiffTreeBuilder2();
        builder.setProjectionDiffer(new XlsProjectionDiffer());

        // 1. Pairs v1:v2
        for (List<DiffPair> pairs : diffGuess.values()) {
            for (DiffPair pair : pairs) {
                checkGrid(pair);
                builder.add(pair);
            }
        }

        // 2. Lonely tables
        // 2.1 v1 only
        for (XlsTable t : tables1) {
            builder.add(new DiffPair(t, null));
        }
        // 2.2 v2 only
        for (XlsTable t : tables2) {
            builder.add(new DiffPair(null, t));
        }

        return builder.compare();
    }

    private void checkGrid(DiffPair pair) {
        var grid1 = pair.getTable1().getTable().getGridTable();
        var grid2 = pair.getTable2().getTable().getGridTable();

        var diff1 = new ArrayList<ICell>();
        var diff2 = new ArrayList<ICell>();

        if (grid1.getWidth() == grid2.getWidth()) {
            compareRows(GridView.of(grid1), GridView.of(grid2), diff1, diff2);
        } else if (grid1.getHeight() == grid2.getHeight()) {
            compareCols(grid1, grid2, diff1, diff2);
        } else {
            compareResized(grid1, grid2, diff1, diff2);
        }

        if (!diff1.isEmpty()) {
            pair.setDiffCells1(diff1);
        }
        if (!diff2.isEmpty()) {
            pair.setDiffCells2(diff2);
        }
    }

    private void compareRows(GridView grid1, GridView grid2, List<ICell> diff1, List<ICell> diff2) {
        var grid1MatchedRows = new ArrayList<Integer>();
        // For each row from grid1, the value of the corresponding row from grid2 (if found)
        // and the difference between them are stored.
        var grid1RowsState = new TreeMap<Integer, RowDiff>();
        // This index is needed so that for each n+1 row from grid1, the corresponding row from grid2 is not lower
        // than the corresponding row from grid2, for row n from grid1.
        var grid2LastMatched = 0;
        // Below we fill grid1RowsState for those rows from grid1 that have a complete match with the rows from grid2
        // if there are no such rows, then the index is set to -1.
        var grid1Height = grid1.height();
        var grid2Height = grid2.height();
        for (var grid1Row = 0; grid1Row < grid1Height; grid1Row++) {
            grid2LastMatched = matchRow(grid1, grid2, grid1Row, grid2LastMatched, grid1MatchedRows, grid1RowsState);
            if (grid1RowsState.get(grid1Row) == null) {
                grid1RowsState.put(grid1Row, new RowDiff().setRowIndex(-1));
            }
        }
        matchUnmatchedRows(grid1, grid2, grid1MatchedRows, grid1RowsState, diff1);
        diff1.addAll(
                grid1RowsState.values().stream().map(RowDiff::getDiff).flatMap(List::stream).collect(Collectors.toList()));
        // For grid2 we compare the rows found for grid1, if there are no such rows, we assume that the row was added.
        for (var grid2Row = 0; grid2Row < grid2Height; grid2Row++) {
            var finalGrid2Row = grid2Row;
            Optional<Integer> matchedKey = grid1RowsState.keySet()
                    .stream()
                    .filter(key -> grid1RowsState.get(key).getRowIndex() == finalGrid2Row)
                    .findFirst();
            if (matchedKey.isPresent()) {
                var rowIndex = matchedKey.get();
                if (!grid1RowsState.get(rowIndex).getDiff().isEmpty()) {
                    diff2.addAll(getDiffs(grid2, grid1, grid2Row, rowIndex));
                }
            } else {
                for (var grid2Col = 0; grid2Col < grid2.width(); grid2Col++) {
                    diff2.add(grid2.cell(grid2Col, grid2Row));
                }
            }
        }
    }

    /**
     * Searches the second grid, from the row after the last matched one, for the row matching the row of the first
     * grid, and records the match in the state of the row. An identical row is recorded unless the next row of the
     * first grid matches it too. A different row is recorded only when both grids have the same height.
     *
     * @return the row of the second grid the search for the next row of the first grid starts from
     */
    private int matchRow(GridView grid1,
                         GridView grid2,
                         int grid1Row,
                         int grid2LastMatched,
                         List<Integer> grid1MatchedRows,
                         Map<Integer, RowDiff> grid1RowsState) {
        var grid1Height = grid1.height();
        var grid2Height = grid2.height();
        var followingRow = true;
        for (var grid2Row = grid2LastMatched; grid2Row < grid2Height; grid2Row++) {
            var diffs = getDiffs(grid1, grid2, grid1Row, grid2Row);
            if (diffs.isEmpty()) {
                // Check if the next line matches the one found.
                // For cases when several identical lines can go in a row.
                var nextRowMatches = !followingRow && grid1Row != grid2Row && grid1Row + 1 < grid1Height
                        && getDiffs(grid1, grid2, grid1Row + 1, grid2Row).isEmpty();
                if (nextRowMatches) {
                    return grid2LastMatched;
                }
                grid1MatchedRows.add(grid1Row);
                grid1RowsState.put(grid1Row, new RowDiff().setRowIndex(grid2Row));
                return grid2Row + 1;
            } else if (grid1Height == grid2Height) {
                grid1RowsState.put(grid1Row, new RowDiff().setRowIndex(grid2Row).setDiff(diffs));
                return grid2Row + 1;
            }
            followingRow = false;
        }
        return grid2LastMatched;
    }

    /**
     * Finds the most similar row of the second grid for each row of the first grid without a match, between the rows
     * matching the previous and the next rows. A row with no candidate rows is taken as deleted.
     */
    private void matchUnmatchedRows(GridView grid1,
                                    GridView grid2,
                                    List<Integer> grid1MatchedRows,
                                    SortedMap<Integer, RowDiff> grid1RowsState,
                                    List<ICell> diff1) {
        // The row of the second grid the closest matched row above is matched with: the rows are walked in order.
        var previousMatch = -1;
        for (var entry : grid1RowsState.entrySet()) {
            var grid1row = entry.getKey();
            var rowState = entry.getValue();
            if (rowState.getRowIndex() != -1) {
                previousMatch = rowState.getRowIndex();
                continue;
            }
            // From and to, the value between which the most suitable string should be found,
            // the range must be between the previous and next found match against grid2.
            int from = previousMatch + 1;
            Optional<Integer> nextMatched = grid1MatchedRows.stream().filter(i -> i > grid1row).findFirst();
            var to = nextMatched.map(i -> grid1RowsState.get(i).getRowIndex()).orElseGet(grid2::height);
            var allRowDiffs = new ArrayList<RowDiff>();
            if (from < to) {
                for (; from < to; from++) {
                    allRowDiffs.add(new RowDiff().setRowIndex(from).setDiff(getDiffs(grid1, grid2, grid1row, from)));
                }
                var minDiff = allRowDiffs.stream().min(Comparator.comparingInt(o -> o.getDiff().size()));
                var rowDiff = minDiff.orElse(new RowDiff());
                rowState.setRowIndex(rowDiff.getRowIndex()).setDiff(rowDiff.getDiff());
                previousMatch = rowDiff.getRowIndex();
            } else {
                // If there are no rows in the range, then we assume that the row was deleted.
                for (var grid1Col = 0; grid1Col < grid1.width(); grid1Col++) {
                    diff1.add(grid1.cell(grid1Col, grid1row));
                }
            }
        }
    }

    private List<ICell> getDiffs(GridView grid1, GridView grid2, int y1, int y2) {
        var diff = new ArrayList<ICell>();
        for (var x = 0; x < grid1.width(); x++) {
            var c1 = grid1.cell(x, y1);
            var c2 = grid2.cell(x, y2);
            if (notEquals(c1, c2)) {
                diff.add(c1);
            }
        }
        return diff;
    }

    private static class RowDiff {

        @Getter(AccessLevel.PRIVATE)
        private int rowIndex;
        @Getter(AccessLevel.PRIVATE)
        private List<ICell> diff = new ArrayList<>();

        private RowDiff setRowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
            return this;
        }

        private RowDiff setDiff(List<ICell> diff) {
            this.diff = diff;
            return this;
        }
    }

    /**
     * Compares two versions of a table that differ in both width and height.
     *
     * <p>The columns are paired first, in their order, by the values their cells share, so a row added to a column
     * does not keep it from its pair. The rows are then compared over the paired columns, as a table keeping its
     * width is. A column without a pair was added or deleted, and every cell of it is marked.
     */
    private void compareResized(IGridTable grid1, IGridTable grid2, List<ICell> diff1, List<ICell> diff2) {
        var pairs = pairColumns(grid1, grid2);
        var columns1 = pairs.stream().mapToInt(pair -> pair[0]).toArray();
        var columns2 = pairs.stream().mapToInt(pair -> pair[1]).toArray();
        compareRows(new GridView(grid1, columns1), new GridView(grid2, columns2), diff1, diff2);
        markUnpairedColumns(grid1, columns1, diff1);
        markUnpairedColumns(grid2, columns2, diff2);
    }

    /**
     * Pairs the columns of two grids, keeping their order, so that the paired columns share as many values as
     * possible. Columns sharing no value are never paired.
     *
     * @return the pairs of the column of the first grid and the column of the second one, from left to right
     */
    private static List<int[]> pairColumns(IGridTable grid1, IGridTable grid2) {
        var width1 = grid1.getWidth();
        var width2 = grid2.getWidth();
        var values2 = IntStream.range(0, width2).mapToObj(column -> valuesOf(grid2, column)).toList();
        var shared = new int[width1][width2];
        for (var column1 = 0; column1 < width1; column1++) {
            var values1 = valuesOf(grid1, column1);
            for (var column2 = 0; column2 < width2; column2++) {
                shared[column1][column2] = sharedCount(values1, values2.get(column2));
            }
        }
        // best[i][j]: the most values the first i columns of the first grid share with the first j of the second.
        var best = new int[width1 + 1][width2 + 1];
        for (var i = 1; i <= width1; i++) {
            for (var j = 1; j <= width2; j++) {
                var paired = shared[i - 1][j - 1] > 0 ? best[i - 1][j - 1] + shared[i - 1][j - 1] : 0;
                best[i][j] = Math.max(paired, Math.max(best[i - 1][j], best[i][j - 1]));
            }
        }
        var pairs = new ArrayList<int[]>();
        var i = width1;
        var j = width2;
        while (i > 0 && j > 0) {
            if (best[i][j] == best[i - 1][j]) {
                i--;
            } else if (best[i][j] == best[i][j - 1]) {
                j--;
            } else {
                i--;
                j--;
                pairs.addFirst(new int[]{i, j});
            }
        }
        return pairs;
    }

    /**
     * How many times each value stands in the column. A merged cell counts once, in the column it starts in: read in
     * every column it spans, a merged header would be a value every two columns share.
     */
    private static Map<Object, Integer> valuesOf(IGridTable grid, int column) {
        var values = new HashMap<Object, Integer>();
        for (var row = 0; row < grid.getHeight(); row++) {
            var cell = grid.getCell(column, row);
            var value = cell.getObjectValue();
            if (value != null && startsAt(cell)) {
                values.merge(value, 1, Integer::sum);
            }
        }
        return values;
    }

    /** Whether the cell is the first cell of its merged region, or is merged with no other. */
    private static boolean startsAt(ICell cell) {
        var region = cell.getRegion();
        return region == null
                || (region.getLeft() == cell.getAbsoluteColumn() && region.getTop() == cell.getAbsoluteRow());
    }

    /** How many values two columns share, a value standing twice in both counted twice. */
    private static int sharedCount(Map<Object, Integer> values1, Map<Object, Integer> values2) {
        var shared = 0;
        for (var entry : values1.entrySet()) {
            shared += Math.min(entry.getValue(), values2.getOrDefault(entry.getKey(), 0));
        }
        return shared;
    }

    /**
     * Marks every cell of the columns without a pair. A merged cell reaching a paired column is compared there, as a
     * cell of that column, and is not marked.
     */
    private static void markUnpairedColumns(IGridTable grid, int[] pairedColumns, List<ICell> diff) {
        var paired = new BitSet(grid.getWidth());
        Arrays.stream(pairedColumns).forEach(paired::set);
        var left = grid.getRegion().getLeft();
        for (var column = paired.nextClearBit(0); column < grid.getWidth(); column = paired.nextClearBit(column + 1)) {
            for (var row = 0; row < grid.getHeight(); row++) {
                var cell = grid.getCell(column, row);
                var region = cell.getRegion();
                if (region == null || !reachesPaired(paired, region.getLeft() - left, region.getRight() - left)) {
                    diff.add(cell);
                }
            }
        }
    }

    /** Whether a paired column stands between the two columns, both included. */
    private static boolean reachesPaired(BitSet paired, int from, int to) {
        var next = paired.nextSetBit(from);
        return next >= 0 && next <= to;
    }

    /**
     * The columns of a grid a comparison reads, in their order: every column of it, or only the ones paired with
     * the columns of another grid.
     */
    private static final class GridView {

        /** The cells, by row and then by column, read once: the rows are compared with many rows of the other grid. */
        private final ICell[][] cells;
        private final int width;

        private GridView(IGridTable table, int[] columns) {
            width = columns.length;
            cells = new ICell[table.getHeight()][];
            for (var row = 0; row < cells.length; row++) {
                var finalRow = row;
                cells[row] = Arrays.stream(columns).mapToObj(column -> table.getCell(column, finalRow))
                        .toArray(ICell[]::new);
            }
        }

        static GridView of(IGridTable table) {
            return new GridView(table, IntStream.range(0, table.getWidth()).toArray());
        }

        int width() {
            return width;
        }

        int height() {
            return cells.length;
        }

        ICell cell(int column, int row) {
            return cells[row][column];
        }
    }

    private void compareCols(IGridTable grid1, IGridTable grid2, List<ICell> diff1, List<ICell> diff2) {
        // compareRows is hard enough :)
        // let reuse it
        var iDiff1 = new ArrayList<ICell>();
        var iDiff2 = new ArrayList<ICell>();
        compareRows(GridView.of(grid1.transpose()), GridView.of(grid2.transpose()), iDiff1, iDiff2);

        // fix diff -- invert coordinates
        for (ICell c : iDiff1) {
            diff1.add(grid1.getCell(c.getRow(), c.getColumn()));
        }
        for (ICell c : iDiff2) {
            diff2.add(grid2.getCell(c.getRow(), c.getColumn()));
        }
    }

    // Which cell attributes besides the value count as a change is a product decision of the diff.
    @SuppressWarnings("java:S1135")
    private boolean notEquals(ICell c1, ICell c2) {
        var o1 = c1.getObjectValue();
        var o2 = c2.getObjectValue();

        // TODO compare value, comment, value and so on...
        if (o1 == null) {
            return o2 != null;
        } else {
            return !o1.equals(o2);
        }
    }
}
