package org.openl.rules.diff.xls2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.openl.OpenClassUtil;
import org.openl.binding.IBoundCode;
import org.openl.classloader.OpenLClassLoader;
import org.openl.conf.UserContext;
import org.openl.impl.DefaultCompileContext;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjectionDiffer;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.types.IOpenClass;
import org.openl.xls.Parser;

/**
 * Find difference between two XLS files. It compares per Table.
 * <p>
 * Incomplete. Need AxB vs CxD implementation. Need to be optimal.
 *
 * @author Aleh Bykhavets
 *
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

    private List<XlsTable> load(IOpenSourceCodeModule src) {
        final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = null;
        try {
            classLoader = new OpenLClassLoader(oldCl);
            Thread.currentThread().setContextClassLoader(classLoader);
            UserContext ucxt = new UserContext(classLoader, ".");

            IParsedCode pc = new Parser(ucxt).parseAsModule(src);
            IBoundCode bc = new XlsBinder(new DefaultCompileContext(), ucxt).bind(pc);
            IOpenClass ioc = bc.getTopNode().getType();

            XlsMetaInfo xmi = (XlsMetaInfo) ioc.getMetaInfo();
            XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();

            TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();
            List<XlsTable> tables = new ArrayList<>(nodes.length);
            for (TableSyntaxNode node : nodes) {
                tables.add(new XlsTable(node));
            }

            return tables;
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
            tables1 = Collections.emptyList();
        } else {
            tables1 = load(new URLSourceCodeModule(URLSourceCodeModule.toUrl(xlsFile1)));
        }
        if (xlsFile2 == null) {
            tables2 = Collections.emptyList();
        } else {
            tables2 = load(new URLSourceCodeModule(URLSourceCodeModule.toUrl(xlsFile2)));
        }
    }

    private void add(String guess, DiffPair r) {
        List<DiffPair> list = diffGuess.computeIfAbsent(guess, e -> new LinkedList<>());
        list.add(r);
    }

    private void diff() {
        // 1. Simple cases
        iterate(new IterClosure() {
            // @Override
            @Override
            public boolean remove(XlsTable t1, XlsTable t2) {
                if (t1.getSheetName().equals(t2.getSheetName())) {
                    String s1 = t1.getLocation().getStart().toString();
                    String s2 = t2.getLocation().getStart().toString();
                    if (s1.equals(s2)) {
                        boolean sameName = t1.getTableName().equals(t2.getTableName());

                        String e1 = t1.getLocation().getEnd().toString();
                        String e2 = t2.getLocation().getEnd().toString();
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
        });

        // 2. Sheet and name seems the same
        iterate(new IterClosure() {
            // @Override
            @Override
            public boolean remove(XlsTable t1, XlsTable t2) {
                if (t1.getSheetName().equals(t2.getSheetName())) {
                    boolean sameName = t1.getTableName().equals(t2.getTableName());
                    if (sameName) {
                        add(GUESS_MAY_BE_SAME, new DiffPair(t1, t2));
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void iterate(IterClosure closure) {
        Iterator<XlsTable> i1 = tables1.iterator();
        while (i1.hasNext()) {
            XlsTable t1 = i1.next();

            Iterator<XlsTable> i2 = tables2.iterator();
            while (i2.hasNext()) {
                XlsTable t2 = i2.next();

                if (closure.remove(t1, t2)) {
                    i1.remove();
                    i2.remove();
                    break;
                }
            }
        }
    }

    private DiffTreeNode buildTree() {
        DiffTreeBuilder2 builder = new DiffTreeBuilder2();
        builder.setProjectionDiffer(new XlsProjectionDiffer());

        // 1. Pairs v1:v2
        for (String guess : diffGuess.keySet()) {
            for (DiffPair pair : diffGuess.get(guess)) {
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
        IGridTable grid1 = pair.getTable1().getTable().getGridTable();
        IGridTable grid2 = pair.getTable2().getTable().getGridTable();

        List<ICell> diff1 = new ArrayList<>();
        List<ICell> diff2 = new ArrayList<>();

        if (grid1.getWidth() == grid2.getWidth() || grid1.getHeight() == grid2.getHeight()) {
            if (grid1.getWidth() == grid2.getWidth()) {
                compareRows(grid1, grid2, diff1, diff2);
            } else {
                compareCols(grid1, grid2, diff1, diff2);
            }
        } else {
            // Diff Size
            // TODO Implement AxB vs CxD algorithm
        }

        if (!diff1.isEmpty()) {
            pair.setDiffCells1(diff1);
        }
        if (!diff2.isEmpty()) {
            pair.setDiffCells2(diff2);
        }
    }
    
    private void compareRows(IGridTable grid1, IGridTable grid2, List<ICell> diff1, List<ICell> diff2) {
        ArrayList<Integer> grid1MatchedRows = new ArrayList<>();
        // For each row from grid1, the value of the corresponding row from grid2 (if found)
        // and the difference between them are stored.
        Map<Integer, RowDiff> grid1RowsState = new HashMap<>();
        // This index is needed so that for each n+1 row from grid1, the corresponding row from grid2 is not lower
        // than the corresponding row from grid2, for row n from grid1.
        int grid2LastMatched = 0;
        // Below we fill grid1RowsState for those rows from grid1 that have a complete match with the rows from grid2
        // if there are no such rows, then the index is set to -1.
        int grid1Height = grid1.getHeight();
        int grid2Height = grid2.getHeight();
        for (int grid1Row = 0; grid1Row < grid1Height; grid1Row++) {
            boolean followingRow = true;
            for (int grid2Row = grid2LastMatched; grid2Row < grid2Height; grid2Row++) {
                List<ICell> diffs = getDiffs(grid1, grid2, grid1Row, grid2Row);
                if (diffs.size() == 0) {
                    if (!followingRow && grid1Row != grid2Row && grid1Row < grid1Height + 1) {
                        // Check if the next line matches the one found.
                        // For cases when several identical lines can go in a row.
                        List<ICell> nextRowDiffs = getDiffs(grid1, grid2, grid1Row + 1, grid2Row);
                        if (nextRowDiffs.size() == 0) {
                            break;
                        }
                    }
                    grid1MatchedRows.add(grid1Row);
                    grid1RowsState.put(grid1Row, new RowDiff().setRowIndex(grid2Row));
                    grid2LastMatched = grid2Row + 1;
                    break;
                } else if (grid1Height == grid2Height) {
                    grid1RowsState.put(grid1Row, new RowDiff().setRowIndex(grid2Row).setDiff(diffs));
                    grid2LastMatched = grid2Row + 1;
                    break;
                }
                followingRow = false;
            }
            if (grid1RowsState.get(grid1Row) == null) {
                grid1RowsState.put(grid1Row, new RowDiff().setRowIndex(-1));
            }
        }
        for (Integer grid1row : grid1RowsState.keySet()) {
            if (grid1RowsState.get(grid1row).getRowIndex() != -1) {
                continue;
            }
            // From and to, the value between which the most suitable string should be found,
            // the range must be between the previous and next found match against grid2.
            int from = grid1row > 1 ? grid1RowsState.get(grid1row - 1).getRowIndex() + 1 : grid1row;
            Optional<Integer> nextMatched = grid1MatchedRows.stream().filter(i -> i > grid1row).findFirst();
            int to = nextMatched.map(i -> grid1RowsState.get(i).getRowIndex()).orElseGet(grid2::getHeight);
            List<RowDiff> allRowDiffs = new ArrayList<>();
            if (from < to) {
                for (; from < to; from++) {
                    allRowDiffs.add(new RowDiff().setRowIndex(from).setDiff(getDiffs(grid1, grid2, grid1row, from)));
                }
                Optional<RowDiff> minDiff = allRowDiffs.stream().min(Comparator.comparingInt(o -> o.getDiff().size()));
                RowDiff rowDiff = minDiff.orElse(new RowDiff());
                grid1RowsState.get(grid1row).setRowIndex(rowDiff.getRowIndex()).setDiff(rowDiff.getDiff());
            } else {
                // If there are no rows in the range, then we assume that the row was deleted.
                for (int grid1Col = 0; grid1Col < grid1.getWidth(); grid1Col++) {
                    diff1.add(grid1.getCell(grid1Col, grid1row));
                }
            }
        }
        diff1.addAll(
            grid1RowsState.values().stream().map(RowDiff::getDiff).flatMap(List::stream).collect(Collectors.toList()));
        // For grid2 we compare the rows found for grid1, if there are no such rows, we assume that the row was added.
        for (int grid2Row = 0; grid2Row < grid2Height; grid2Row++) {
            int finalGrid2Row = grid2Row;
            Optional<Integer> matchedKey = grid1RowsState.keySet()
                .stream()
                .filter(key -> grid1RowsState.get(key).getRowIndex() == finalGrid2Row)
                .findFirst();
            if (matchedKey.isPresent()) {
                int rowIndex = matchedKey.get();
                if (!grid1RowsState.get(rowIndex).getDiff().isEmpty()) {
                    diff2.addAll(getDiffs(grid2, grid1, grid2Row, rowIndex));
                }
            } else {
                for (int grid2Col = 0; grid2Col < grid2.getWidth(); grid2Col++) {
                    diff2.add(grid2.getCell(grid2Col, grid2Row));
                }
            }
        }
    }

    private List<ICell> getDiffs(IGridTable grid1, IGridTable grid2, int y1, int y2) {
        List<ICell> diff = new ArrayList<>();
        for (int x = 0; x < grid1.getWidth(); x++) {
            ICell c1 = grid1.getCell(x, y1);
            ICell c2 = grid2.getCell(x, y2);
            if (notEquals(c1, c2)) {
                diff.add(c1);
            }
        }
        return diff;
    }

    private static class RowDiff {

        private int rowIndex;
        private List<ICell> diff = new ArrayList<>();

        public int getRowIndex() {
            return rowIndex;
        }

        public List<ICell> getDiff() {
            return diff;
        }

        public RowDiff setRowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
            return this;
        }

        public RowDiff setDiff(List<ICell> diff) {
            this.diff = diff;
            return this;
        }
    }

    private void compareCols(IGridTable grid1, IGridTable grid2, List<ICell> diff1, List<ICell> diff2) {
        // compareRows is hard enough :)
        // let reuse it
        List<ICell> iDiff1 = new ArrayList<>();
        List<ICell> iDiff2 = new ArrayList<>();
        compareRows(grid1.transpose(), grid2.transpose(), iDiff1, iDiff2);

        // fix diff -- invert coordinates
        for (ICell c : iDiff1) {
            diff1.add(grid1.getCell(c.getRow(), c.getColumn()));
        }
        for (ICell c : iDiff2) {
            diff2.add(grid2.getCell(c.getRow(), c.getColumn()));
        }
    }

    private boolean notEquals(ICell c1, ICell c2) {
        Object o1 = c1.getObjectValue();
        Object o2 = c2.getObjectValue();

        // TODO compare value, comment, value and so on...
        if (o1 == null) {
            return o2 != null;
        } else {
            return !o1.equals(o2);
        }
    }
}
