package org.openl.rules.xls.merge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.util.IOUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.compare.ChangeStatus;
import org.openl.rules.xls.merge.compare.LineChange;
import org.openl.rules.xls.merge.compare.SheetComparison;
import org.openl.rules.xls.merge.compare.TableComparison;
import org.openl.rules.xls.merge.compare.WorkbookComparison;

/**
 * Compares two versions of a workbook table by table, the way the merge finds the tables and lines them up.
 *
 * <p>A sheet is found by its name. The tables of a sheet are found by their headers and their places, as the merge
 * finds them, so a table moved on its sheet is still the same table. A table found at the same place with a header
 * of another kind, such as a {@code Datatype} table where a {@code Data} table was, is another table. Cells outside
 * OpenL tables have no kind: a block of them found at the same place is the same block, whatever it holds. Tables that
 * share a header and are not found otherwise are paired in their order from top to bottom. A table renamed and moved
 * at once is found by the rest of its header and by the values it keeps.
 *
 * <p>A table pushed by rows inserted or deleted above it is not moved. A table above it grew or shrank by as many
 * rows, or a table was added where it stood, or removed from where it stands now.
 *
 * <p>The rows and the columns of a table are lined up the way a text comparison lines up lines. A table that keeps its
 * width lines up its rows, and one that keeps its height its columns. A table that changes both pairs its columns by
 * the values they share first, and lines up its rows over the paired columns. A run of changed lines is paired by the
 * values the lines share, so a line removed and another one added do not read as every line between them changed; a
 * line of a single value replaced by another value is that line changed. Too
 * many changed lines to line them up are paired by their order.
 *
 * <p>The workbooks are read as they are. Nothing in them is compiled.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class XlsWorkbookComparator {

    /**
     * The first words of the headers of OpenL tables, in lower case, as the rules engine reads them
     * ({@code IXlsTableNames}, which this module does not depend on).
     */
    private static final Set<String> OPENL_KINDS = Set.of("dt", "rules", "simplerules", "smartrules", "simplelookup",
            "smartlookup", "constants", "conditions", "actions", "returns", "spreadsheet", "calc", "tbasic", "algorithm",
            "columnmatch", "properties", "code", "method", "data", "datatype", "environment", "testmethod", "test",
            "runmethod", "run", "tablepart");

    /**
     * Compares two versions of a workbook.
     *
     * @param first  the first version, the one the second is compared against
     * @param second the second version
     * @return what the comparison found, with both versions open until it is closed
     * @throws IOException when a version cannot be read as a workbook
     */
    public static WorkbookComparison compare(File first, File second) throws IOException {
        Workbook firstBook = null;
        Workbook secondBook = null;
        try {
            firstBook = open(first);
            secondBook = open(second);
            return new WorkbookComparison(sheets(firstBook, secondBook), firstBook, secondBook);
        } catch (IOException | RuntimeException e) {
            IOUtils.closeQuietly(firstBook);
            IOUtils.closeQuietly(secondBook);
            throw e;
        }
    }

    /**
     * Opens a version of a workbook to read.
     *
     * @throws IOException when the file cannot be read as a workbook
     */
    static Workbook open(File file) throws IOException {
        return StreamWorkbook.open(file, true);
    }

    /**
     * Compares two open versions of a workbook sheet by sheet.
     *
     * @return the sheets of the first version in its order, then the sheets only the second version holds
     */
    static List<SheetComparison> sheets(Workbook first, Workbook second) {
        var sheets = new ArrayList<SheetComparison>();
        for (Sheet sheet : first) {
            var other = second.getSheet(sheet.getSheetName());
            sheets.add(other == null
                    ? oneSided(sheet, ChangeStatus.REMOVED)
                    : compareSheets(new Cursor(first, sheet), new Cursor(second, other)));
        }
        for (Sheet sheet : second) {
            if (first.getSheet(sheet.getSheetName()) == null) {
                sheets.add(oneSided(sheet, ChangeStatus.ADDED));
            }
        }
        return sheets;
    }

    /**
     * A sheet only one version holds, with every table of it.
     */
    private static SheetComparison oneSided(Sheet sheet, ChangeStatus status) {
        var tables = XlsSheetTables.split(sheet)
                .stream()
                .map(table -> TableComparison.oneSided(XlsSheetTables.header(sheet, table), table, status))
                .toList();
        return new SheetComparison(sheet.getSheetName(), status, tables);
    }

    private static SheetComparison compareSheets(Cursor first, Cursor second) {
        var firstTables = XlsSheetTables.split(first.sheet);
        var secondTables = XlsSheetTables.split(second.sheet);
        var pairs = new HashMap<>(XlsSheetTables.match(firstTables, first.sheet, secondTables, second.sheet));
        pairs.entrySet().removeIf(pair -> !sameKind(XlsSheetTables.header(first.sheet, pair.getKey()),
                XlsSheetTables.header(second.sheet, pair.getValue())));
        pairByHeaders(firstTables, first.sheet, secondTables, second.sheet, pairs);
        pairRenamed(first, firstTables, second, secondTables, pairs);
        var paired = new HashSet<>(pairs.values());
        var added = secondTables.stream().filter(table -> !paired.contains(table)).toList();
        var removed = firstTables.stream().filter(table -> !pairs.containsKey(table)).toList();
        var moved = movedOnPurpose(firstTables, pairs, added, removed);

        var placed = new ArrayList<Placed>();
        for (var table : firstTables) {
            var pair = pairs.get(table);
            if (pair == null) {
                var header = XlsSheetTables.header(first.sheet, table);
                placed.add(new Placed(table, TableComparison.oneSided(header, table, ChangeStatus.REMOVED)));
            } else {
                placed.add(new Placed(pair, compareTables(first, table, second, pair, moved.contains(table))));
            }
        }
        for (var table : added) {
            var header = XlsSheetTables.header(second.sheet, table);
            placed.add(new Placed(table, TableComparison.oneSided(header, table, ChangeStatus.ADDED)));
        }
        placed.sort(Comparator.comparingInt((Placed table) -> table.place().getFirstRow())
                .thenComparingInt(table -> table.place().getFirstColumn()));
        var tables = placed.stream().map(Placed::table).toList();
        var status = tables.stream().allMatch(table -> table.status() == ChangeStatus.EQUAL)
                ? ChangeStatus.EQUAL
                : ChangeStatus.CHANGED;
        return new SheetComparison(first.sheet.getSheetName(), status, tables);
    }

    /**
     * A compared table and the place it is listed by.
     */
    private record Placed(CellRangeAddress place, TableComparison table) {
    }

    /**
     * Tells whether two headers name tables of one kind. The first word of an OpenL table header, such as
     * {@code SmartRules} or {@code Datatype}, is the kind of the table. A table found at the same place with a
     * header of another kind is another table put where the first one was. Cells outside OpenL tables, such as notes
     * or formulas beside them, have no kind, so they are of one kind with each other.
     */
    private static boolean sameKind(String header, String otherHeader) {
        return header.equals(otherHeader) || kindOf(header).equals(kindOf(otherHeader));
    }

    /**
     * Whether a table found in a workbook is an OpenL table: its header starts with the word of a kind of OpenL table.
     * Cells outside OpenL tables, such as notes or formulas beside them, are not.
     *
     * @param header the header of the table, as its first cell reads
     */
    public static boolean isOpenLTable(String header) {
        return !kindOf(header).isEmpty();
    }

    /** The kind of a table: the first word of its header, or nothing for cells outside OpenL tables. */
    private static String kindOf(String header) {
        var end = header.indexOf(' ');
        var kind = (end < 0 ? header : header.substring(0, end)).toLowerCase(Locale.ROOT);
        return OPENL_KINDS.contains(kind) ? kind : "";
    }

    /**
     * Pairs the tables left without a pair that were renamed and moved at once. Their headers differ in the name alone:
     * the kind, the type of the result and the parameters stay. And they share at least half of the values of the
     * smaller table under the header, as a table renamed keeps most of its rules.
     *
     * <p>Of several such tables of the second version, a table of the first one is paired with the one sharing the most
     * values. The tables are taken from top to bottom.
     */
    private static void pairRenamed(Cursor first,
                                    List<CellRangeAddress> firstTables,
                                    Cursor second,
                                    List<CellRangeAddress> secondTables,
                                    Map<CellRangeAddress, CellRangeAddress> pairs) {
        var paired = new HashSet<>(pairs.values());
        var candidates = secondTables.stream().filter(table -> !paired.contains(table)).toList();
        var unpaired = firstTables.stream().filter(table -> !pairs.containsKey(table)).toList();
        if (candidates.isEmpty() || unpaired.isEmpty()) {
            return;
        }
        var taken = new HashSet<CellRangeAddress>();
        var candidateValues = new HashMap<CellRangeAddress, Map<Long, Integer>>();
        for (var table : unpaired) {
            var signature = signatureOf(XlsSheetTables.header(first.sheet, table));
            var values = valuesOf(first, table);
            CellRangeAddress best = null;
            var bestShared = 0;
            for (var candidate : candidates) {
                var sameSignature = !signature.isEmpty()
                        && signature.equals(signatureOf(XlsSheetTables.header(second.sheet, candidate)));
                var free = !taken.contains(candidate);
                var shared = sameSignature && free
                        ? sharedValues(values, candidateValues.computeIfAbsent(candidate, key -> valuesOf(second, key)))
                        : 0;
                if (shared > bestShared) {
                    best = candidate;
                    bestShared = shared;
                }
            }
            if (best != null) {
                pairs.put(table, best);
                taken.add(best);
            }
        }
    }

    /**
     * The header of a table without the name of the table, such as {@code SmartRules Double (String species)}, or
     * nothing for a header that takes no parameters.
     */
    private static String signatureOf(String header) {
        var open = header.indexOf('(');
        if (open < 0) {
            return "";
        }
        var nameStart = header.lastIndexOf(' ', open - 1) + 1;
        return header.substring(0, nameStart) + header.substring(open).replace(" ", "");
    }

    /**
     * How many values of the smaller of two tables the other one holds as well, or nothing when it is less than half of
     * them.
     */
    private static int sharedValues(Map<Long, Integer> values, Map<Long, Integer> otherValues) {
        var shared = ColumnValues.sharedCount(values, otherValues);
        var smaller = Math.min(count(values), count(otherValues));
        return smaller > 0 && 2 * shared >= smaller ? shared : 0;
    }

    private static int count(Map<Long, Integer> values) {
        return values.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * The values a table shows under its header, each counted as often as the table shows it.
     */
    private static Map<Long, Integer> valuesOf(Cursor cursor, CellRangeAddress table) {
        var lines = new XlsTableMerger.View(cursor, table, false).shownDigests();
        var values = new HashMap<Long, Integer>();
        for (var line = 1; line < lines.length; line++) {
            for (var value : lines[line]) {
                if (value != 0) {
                    values.merge(value, 1, Integer::sum);
                }
            }
        }
        return values;
    }

    /**
     * Pairs the tables left without a pair that share a header, in their order from top to bottom. The versions of a
     * rule told apart by their properties keep their header, and rows inserted above them push them together.
     */
    private static void pairByHeaders(List<CellRangeAddress> firstTables,
                                      Sheet firstSheet,
                                      List<CellRangeAddress> secondTables,
                                      Sheet secondSheet,
                                      Map<CellRangeAddress, CellRangeAddress> pairs) {
        var paired = new HashSet<>(pairs.values());
        var byHeader = new LinkedHashMap<String, Queue<CellRangeAddress>>();
        secondTables.stream()
                .filter(table -> !paired.contains(table))
                .forEach(table -> byHeader.computeIfAbsent(XlsSheetTables.header(secondSheet, table),
                        header -> new ArrayDeque<>()).add(table));
        byHeader.remove("");
        firstTables.stream().filter(table -> !pairs.containsKey(table)).forEach(table -> {
            var candidates = byHeader.get(XlsSheetTables.header(firstSheet, table));
            if (candidates != null && !candidates.isEmpty()) {
                pairs.put(table, candidates.remove());
            }
        });
    }

    /**
     * The tables of the first version the second one moved on purpose. A table moved into another column is moved on
     * purpose. So is a table moved by rows that neither a table above nor a table added or removed above explains.
     *
     * @param firstTables the tables of the first version, from top to bottom and from left to right
     * @param added       the tables only the second version holds
     * @param removed     the tables only the first version holds
     */
    private static Set<CellRangeAddress> movedOnPurpose(List<CellRangeAddress> firstTables,
                                                        Map<CellRangeAddress, CellRangeAddress> pairs,
                                                        List<CellRangeAddress> added,
                                                        List<CellRangeAddress> removed) {
        var kept = firstTables.stream().filter(pairs::containsKey).toList();
        var moved = new HashSet<CellRangeAddress>();
        for (var i = 0; i < kept.size(); i++) {
            var table = kept.get(i);
            var place = pairs.get(table);
            var shift = place.getFirstRow() - table.getFirstRow();
            var pushed = place.getFirstColumn() == table.getFirstColumn()
                    && (shift == 0
                    || explains(-1, -1, 0, table, place, added, removed)
                    || kept.subList(0, i).stream().anyMatch(above -> explains(above, pairs.get(above), table, place,
                    added, removed)));
            if (!pushed) {
                moved.add(table);
            }
        }
        return moved;
    }

    /**
     * Tells whether a table above explains the rows a table moved by. It does when that table moved as far, or as far
     * once it grew or shrank. It does as well when the rows between the two hold a table added or removed.
     */
    private static boolean explains(CellRangeAddress other,
                                    CellRangeAddress otherPlace,
                                    CellRangeAddress table,
                                    CellRangeAddress place,
                                    List<CellRangeAddress> added,
                                    List<CellRangeAddress> removed) {
        var otherShift = otherPlace.getFirstRow() - other.getFirstRow();
        var growth = XlsTableMerger.height(otherPlace) - XlsTableMerger.height(other);
        var shift = place.getFirstRow() - table.getFirstRow();
        var expected = otherShift + growth;
        return shift == otherShift
                || explains(other.getLastRow(), otherPlace.getLastRow(), expected, table, place, added, removed);
    }

    /**
     * Tells whether the rows between a row and a table explain the rows the table moved by, more than the given ones:
     * a table added between them pushes it down, a table removed between them pulls it up.
     *
     * <p>An added table pushes the table only from where the table would stand without it: one added lower, in rows
     * the table never stood in, did not push it. A removed table pulls the table only into the rows it held.
     *
     * @param firstRow  the row of the first version the rows start after, {@code -1} for the top of the sheet
     * @param secondRow the row of the second version the rows start after, {@code -1} for the top of the sheet
     * @param expected  the rows the table moves by when nothing between them changed
     */
    private static boolean explains(int firstRow,
                                    int secondRow,
                                    int expected,
                                    CellRangeAddress table,
                                    CellRangeAddress place,
                                    List<CellRangeAddress> added,
                                    List<CellRangeAddress> removed) {
        var shift = place.getFirstRow() - table.getFirstRow();
        var wouldStand = table.getFirstRow() + expected;
        return shift == expected
                || shift > expected && anyBetween(added, secondRow, place.getFirstRow(),
                pusher -> pusher.getFirstRow() <= wouldStand)
                || shift < expected && anyBetween(removed, firstRow, table.getFirstRow(),
                puller -> puller.getLastRow() >= place.getFirstRow() - expected);
    }

    private static boolean anyBetween(List<CellRangeAddress> tables,
                                      int afterRow,
                                      int beforeRow,
                                      Predicate<CellRangeAddress> pushes) {
        return tables.stream()
                .anyMatch(table -> table.getFirstRow() > afterRow && table.getLastRow() < beforeRow
                        && pushes.test(table));
    }

    /**
     * Compares a table both versions hold.
     *
     * @param moved whether the second version moved the table on purpose
     */
    private static TableComparison compareTables(Cursor first,
                                                 CellRangeAddress firstTable,
                                                 Cursor second,
                                                 CellRangeAddress secondTable,
                                                 boolean moved) {
        var firstView = new XlsTableMerger.View(first, firstTable, false);
        var secondView = new XlsTableMerger.View(second, secondTable, false);
        var columns = columns(firstView, secondView);
        var rows = rows(firstView, secondView, columns);
        var cells = new Cells(first, firstTable, second, secondTable, rows, columns);
        var rowChanges = Cells.lines(rows, cells.changedRows);
        var columnChanges = Cells.lines(columns, cells.changedColumns);
        var equal = LineChange.allEqual(rowChanges) && LineChange.allEqual(columnChanges);
        ChangeStatus status;
        if (!equal || cells.formattingChanged) {
            status = ChangeStatus.CHANGED;
        } else {
            status = moved ? ChangeStatus.MOVED : ChangeStatus.EQUAL;
        }
        return new TableComparison(XlsSheetTables.header(second.sheet, secondTable),
                firstTable,
                secondTable,
                status,
                moved,
                cells.formattingChanged,
                rowChanges,
                columnChanges,
                cells.firstChanged,
                cells.secondChanged);
    }

    /**
     * The lines of the first version of a table paired with the lines of the second one, in the same order in both.
     *
     * @param sideOf the line of the second version by the line of the first one, {@code -1} for none
     * @param baseOf the line of the first version by the line of the second one, {@code -1} for none
     */
    @RequiredArgsConstructor
    private static final class Pairing {
        private final int[] sideOf;
        private final int[] baseOf;

        int[] sideOf() {
            return sideOf;
        }

        int[] baseOf() {
            return baseOf;
        }

        /**
         * The first lines of both paired by their order, the rest of the longer one unpaired.
         */
        static Pairing inOrder(int count, int otherCount) {
            var pairing = none(count, otherCount);
            for (var line = 0; line < Math.min(count, otherCount); line++) {
                pairing.pair(line, line);
            }
            return pairing;
        }

        /**
         * No line paired yet.
         */
        static Pairing none(int count, int otherCount) {
            var sideOf = new int[count];
            var baseOf = new int[otherCount];
            Arrays.fill(sideOf, -1);
            Arrays.fill(baseOf, -1);
            return new Pairing(sideOf, baseOf);
        }

        void pair(int line, int otherLine) {
            sideOf[line] = otherLine;
            baseOf[otherLine] = line;
        }

        static Pairing of(@Nullable LineAlignment alignment, int count, int otherCount) {
            return alignment == null ? inOrder(count, otherCount) : new Pairing(alignment.sideOf(), alignment.baseOf());
        }

        /**
         * The paired lines of the first version, in order.
         */
        int[] pairedFirst() {
            return IntStream.range(0, sideOf.length).filter(line -> sideOf[line] >= 0).toArray();
        }

        /**
         * The paired lines of the second version, in order.
         */
        int[] pairedSecond() {
            return IntStream.range(0, baseOf.length).filter(line -> baseOf[line] >= 0).toArray();
        }
    }

    /**
     * Pairs the columns of two versions of a table. A table that keeps its width pairs all of them in their order. A
     * table that keeps its height lines them up. Any other table pairs them by the values they share.
     */
    private static Pairing columns(XlsTableMerger.View first, XlsTableMerger.View second) {
        var width = XlsTableMerger.width(first.table());
        var otherWidth = XlsTableMerger.width(second.table());
        if (width == otherWidth) {
            return Pairing.inOrder(width, otherWidth);
        } else if (XlsTableMerger.height(first.table()) == XlsTableMerger.height(second.table())) {
            var firstColumns = new XlsTableMerger.View(first.cursor(), first.table(), true);
            var secondColumns = new XlsTableMerger.View(second.cursor(), second.table(), true);
            return Pairing.of(align(firstColumns.cellDigests(),
                    firstColumns.shownDigests(),
                    secondColumns.cellDigests(),
                    secondColumns.shownDigests()), width, otherWidth);
        }
        return ColumnValues.pair(first, second);
    }

    /**
     * Pairs the rows of two versions of a table. A table that keeps its height while its width changes pairs all of
     * them in their order. Any other table lines them up by the cells of the paired columns.
     */
    private static Pairing rows(XlsTableMerger.View first, XlsTableMerger.View second, Pairing columns) {
        var height = XlsTableMerger.height(first.table());
        var otherHeight = XlsTableMerger.height(second.table());
        if (height == otherHeight && XlsTableMerger.width(first.table()) != XlsTableMerger.width(second.table())) {
            return Pairing.inOrder(height, otherHeight);
        }
        var firstColumns = columns.pairedFirst();
        var secondColumns = columns.pairedSecond();
        return Pairing.of(align(only(first.cellDigests(), firstColumns),
                only(first.shownDigests(), firstColumns),
                only(second.cellDigests(), secondColumns),
                only(second.shownDigests(), secondColumns)), height, otherHeight);
    }

    private static @Nullable LineAlignment align(long[][] cells,
                                                 long[][] shown,
                                                 long[][] otherCells,
                                                 long[][] otherShown) {
        return LineAlignment.alignByContent(XlsTableMerger.lineDigests(cells),
                XlsTableMerger.lineDigests(otherCells),
                similarity(shown, otherShown));
    }

    /**
     * Tells how alike two lines are, as the merge tells it. A line of a single value, as a value of a vocabulary is,
     * shares no value with another one and has nothing else to tell it by: such a line is alike any other line of a
     * single value, so a value changed into another is the line changed in place, also next to a line added.
     */
    private static LineAlignment.Similarity similarity(long[][] shown, long[][] otherShown) {
        var cells = XlsTableMerger.similarity(shown, otherShown);
        return (line, otherLine) -> {
            var shared = cells.of(line, otherLine);
            return shared < 0 && filled(shown[line]) == 1 && filled(otherShown[otherLine]) == 1 ? 0 : shared;
        };
    }

    /** How many cells of a line show a value. */
    private static long filled(long[] digests) {
        return Arrays.stream(digests).filter(digest -> digest != 0).count();
    }

    /**
     * The digests of the cells of every line at the given positions only.
     */
    private static long[][] only(long[][] digests, int[] positions) {
        var kept = new long[digests.length][];
        for (var line = 0; line < digests.length; line++) {
            var cells = digests[line];
            kept[line] = Arrays.stream(positions).mapToLong(position -> cells[position]).toArray();
        }
        return kept;
    }

    /**
     * Pairs the columns of two versions of a table that changed both its width and its height by the values their
     * cells share, whatever row a value stands in.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ColumnValues {

        /**
         * Pairs the columns, keeping their order, so that the paired columns share as many values as possible. Columns
         * sharing no value are never paired.
         */
        static Pairing pair(XlsTableMerger.View first, XlsTableMerger.View second) {
            var values = valuesOf(first);
            var otherValues = valuesOf(second);
            var width = values.size();
            var otherWidth = otherValues.size();
            // best[i][j]: the most values the first i columns of the first version share with the first j of the other
            var best = new int[width + 1][otherWidth + 1];
            var shared = new int[width][otherWidth];
            for (var i = 1; i <= width; i++) {
                for (var j = 1; j <= otherWidth; j++) {
                    shared[i - 1][j - 1] = sharedCount(values.get(i - 1), otherValues.get(j - 1));
                    var paired = shared[i - 1][j - 1] > 0 ? best[i - 1][j - 1] + shared[i - 1][j - 1] : 0;
                    best[i][j] = Math.max(paired, Math.max(best[i - 1][j], best[i][j - 1]));
                }
            }
            var pairing = Pairing.none(width, otherWidth);
            var i = width;
            var j = otherWidth;
            while (i > 0 && j > 0) {
                if (best[i][j] == best[i - 1][j]) {
                    i--;
                } else if (best[i][j] == best[i][j - 1]) {
                    j--;
                } else {
                    i--;
                    j--;
                    pairing.pair(i, j);
                }
            }
            return pairing;
        }

        /**
         * How many times each value stands in each column. A merged cell counts once, in the column it starts in:
         * read in every column it spans, a merged header would be a value every two columns share.
         */
        private static List<Map<Long, Integer>> valuesOf(XlsTableMerger.View view) {
            var columns = new XlsTableMerger.View(view.cursor(), view.table(), true);
            var digests = columns.cellDigests();
            var values = new ArrayList<Map<Long, Integer>>();
            for (var column = 0; column < digests.length; column++) {
                var counts = new HashMap<Long, Integer>();
                for (var row = 0; row < digests[column].length; row++) {
                    var address = columns.address(column, row);
                    var region = view.cursor().getMergedRegion(address.getRow(), address.getColumn());
                    var startsHere = region == null || region.getFirstRow() == address.getRow()
                            && region.getFirstColumn() == address.getColumn();
                    if (digests[column][row] != 0 && startsHere) {
                        counts.merge(digests[column][row], 1, Integer::sum);
                    }
                }
                values.add(counts);
            }
            return values;
        }

        /**
         * How many values two lines or two tables share, a value standing twice in both counted twice.
         */
        private static int sharedCount(Map<Long, Integer> values, Map<Long, Integer> otherValues) {
            var shared = 0;
            for (var entry : values.entrySet()) {
                shared += Math.min(entry.getValue(), otherValues.getOrDefault(entry.getKey(), 0));
            }
            return shared;
        }
    }

    /**
     * The cells of the paired rows and columns of a table compared one with the other.
     */
    private static final class Cells {

        private final List<CellAddress> firstChanged = new ArrayList<>();
        private final List<CellAddress> secondChanged = new ArrayList<>();
        /**
         * The rows of the first version that hold a cell that differs.
         */
        private final boolean[] changedRows;
        /**
         * The columns of the first version that hold a cell that differs.
         */
        private final boolean[] changedColumns;
        private final Cursor first;
        private final CellRangeAddress firstTable;
        private final Cursor second;
        private final CellRangeAddress secondTable;
        private final Pairing rows;
        private final Pairing columns;
        private boolean formattingChanged;

        private Cells(Cursor first,
                      CellRangeAddress firstTable,
                      Cursor second,
                      CellRangeAddress secondTable,
                      Pairing rows,
                      Pairing columns) {
            this.first = first;
            this.firstTable = firstTable;
            this.second = second;
            this.secondTable = secondTable;
            this.rows = rows;
            this.columns = columns;
            changedRows = new boolean[XlsTableMerger.height(firstTable)];
            changedColumns = new boolean[XlsTableMerger.width(firstTable)];
            var pairedColumns = columns.pairedFirst();
            for (var row : rows.pairedFirst()) {
                for (var column : pairedColumns) {
                    compare(row, column);
                }
            }
        }

        private void compare(int row, int column) {
            var firstCell = new CellAddress(firstTable.getFirstRow() + row, firstTable.getFirstColumn() + column);
            var secondCell = new CellAddress(secondTable.getFirstRow() + rows.sideOf()[row],
                    secondTable.getFirstColumn() + columns.sideOf()[column]);
            if (!sameContent(firstCell, secondCell)) {
                firstChanged.add(firstCell);
                secondChanged.add(secondCell);
                changedRows[row] = true;
                changedColumns[column] = true;
            }
            first.cell = SheetUtil.getCell(first.sheet, firstCell.getRow(), firstCell.getColumn());
            second.cell = SheetUtil.getCell(second.sheet, secondCell.getRow(), secondCell.getColumn());
            formattingChanged |= !XlsSheetsMatcher.equalStyles(first, second);
            first.cell = second.cell = null;
        }

        /**
         * The lines of both versions in the order they are read, a removed line before the line added in its place.
         *
         * @param changed the lines of the first version that hold a cell that differs
         */
        private static List<LineChange> lines(Pairing pairing, boolean[] changed) {
            var lines = new ArrayList<LineChange>();
            var line = 0;
            var otherLine = 0;
            while (line < pairing.sideOf().length || otherLine < pairing.baseOf().length) {
                if (line < pairing.sideOf().length && pairing.sideOf()[line] < 0) {
                    lines.add(new LineChange(line++, LineChange.NONE, ChangeStatus.REMOVED));
                } else if (otherLine < pairing.baseOf().length && pairing.baseOf()[otherLine] < 0) {
                    lines.add(new LineChange(LineChange.NONE, otherLine++, ChangeStatus.ADDED));
                } else {
                    var status = changed[line] ? ChangeStatus.CHANGED : ChangeStatus.EQUAL;
                    lines.add(new LineChange(line++, otherLine++, status));
                }
            }
            return lines;
        }

        /**
         * Tells whether two cells show the same. They do when their merging, their value or formula, the result of
         * their formula and their comment are the same. A formula kept as it was computes another result once a cell it
         * reads changed, which Excel then shows.
         *
         * <p>A cell merged cells cover shows the first of the merged cells, whatever it keeps itself. Merged cells
         * are the same when they cover the same paired rows and columns. Merged cells stretched over the lines one
         * version added, or shrunk off the lines it removed, are the same merged cells, and so is a cell left alone
         * once the lines it was merged with are removed.
         */
        private boolean sameContent(CellAddress firstCell, CellAddress secondCell) {
            var area = areaOf(first, firstCell);
            var otherArea = areaOf(second, secondCell);
            if (!sameMerging(area, otherArea)) {
                return false;
            }
            first.cell = SheetUtil.getCell(first.sheet, area.getFirstRow(), area.getFirstColumn());
            second.cell = SheetUtil.getCell(second.sheet, otherArea.getFirstRow(), otherArea.getFirstColumn());
            var same = XlsSheetsMatcher.equalValues(first, second)
                    && sameResult(first.cell, second.cell)
                    && XlsSheetsMatcher.equalComments(first, second);
            first.cell = second.cell = null;
            return same;
        }

        /**
         * Tells whether two cells compute the same result, as the workbooks keep it. A cell without a formula computes
         * none.
         */
        private static boolean sameResult(@Nullable Cell cell, @Nullable Cell other) {
            var formulas = cell != null && other != null
                    && cell.getCellType() == CellType.FORMULA && other.getCellType() == CellType.FORMULA;
            if (!formulas) {
                return true;
            }
            var type = cell.getCachedFormulaResultType();
            if (type != other.getCachedFormulaResultType()) {
                return false;
            }
            return switch (type) {
                case NUMERIC -> cell.getNumericCellValue() == other.getNumericCellValue();
                case STRING -> cell.getStringCellValue().equals(other.getStringCellValue());
                case BOOLEAN -> cell.getBooleanCellValue() == other.getBooleanCellValue();
                case ERROR -> cell.getErrorCellValue() == other.getErrorCellValue();
                default -> true;
            };
        }

        /**
         * The merged cells a cell belongs to, or the cell alone when no merged cells cover it.
         */
        private static CellRangeAddress areaOf(Cursor cursor, CellAddress cell) {
            var region = cursor.getMergedRegion(cell.getRow(), cell.getColumn());
            return region != null
                    ? region
                    : new CellRangeAddress(cell.getRow(), cell.getRow(), cell.getColumn(), cell.getColumn());
        }

        /**
         * Tells whether two areas cover the same paired rows and columns.
         */
        private boolean sameMerging(CellRangeAddress area, CellRangeAddress otherArea) {
            return sameLines(area.getFirstRow() - firstTable.getFirstRow(),
                    area.getLastRow() - firstTable.getFirstRow(),
                    otherArea.getFirstRow() - secondTable.getFirstRow(),
                    otherArea.getLastRow() - secondTable.getFirstRow(),
                    rows)
                    && sameLines(area.getFirstColumn() - firstTable.getFirstColumn(),
                    area.getLastColumn() - firstTable.getFirstColumn(),
                    otherArea.getFirstColumn() - secondTable.getFirstColumn(),
                    otherArea.getLastColumn() - secondTable.getFirstColumn(),
                    columns);
        }

        /**
         * Tells whether the paired lines within two spans of lines are paired with each other.
         */
        private static boolean sameLines(int from, int to, int otherFrom, int otherTo, Pairing pairing) {
            return pairedWithin(from, to, pairing.sideOf(), otherFrom, otherTo)
                    && pairedWithin(otherFrom, otherTo, pairing.baseOf(), from, to);
        }

        /**
         * Tells whether every paired line of a span is paired with a line of the other span.
         */
        private static boolean pairedWithin(int firstLine, int lastLine, int[] pairs, int firstPair, int lastPair) {
            return IntStream.rangeClosed(Math.max(firstLine, 0), Math.min(lastLine, pairs.length - 1))
                    .map(line -> pairs[line])
                    .allMatch(other -> other < 0 || other >= firstPair && other <= lastPair);
        }

    }
}
