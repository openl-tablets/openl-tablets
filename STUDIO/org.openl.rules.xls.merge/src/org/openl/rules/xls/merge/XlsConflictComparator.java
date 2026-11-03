package org.openl.rules.xls.merge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.util.IOUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.compare.ChangeStatus;
import org.openl.rules.xls.merge.compare.ConflictComparison;
import org.openl.rules.xls.merge.compare.ConflictLine;
import org.openl.rules.xls.merge.compare.ConflictSheet;
import org.openl.rules.xls.merge.compare.ConflictTable;
import org.openl.rules.xls.merge.compare.LineChange;
import org.openl.rules.xls.merge.compare.SheetComparison;
import org.openl.rules.xls.merge.compare.TableComparison;

/**
 * Compares the base version of a workbook with the two versions of a conflict, as a merge sees them.
 *
 * <p>Each version is compared with the base the way {@link XlsWorkbookComparator} compares two versions. The two
 * comparisons are joined by the base: a sheet by its name, a table by its place in the base, and a row or a column by
 * the line of the base it stands for. A table or a line a version added stands where that version put it; the lines
 * both versions inserted alike at one place of the base are one line.
 *
 * <p>A cell both versions changed is a conflict when the values they hold differ.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class XlsConflictComparator {

    /** The most pairs of lines inserted at one place that are told apart: runs longer than that stand apart. */
    private static final long MAX_PAIRED = 1_000_000;

    /**
     * Compares the base version of a workbook with the two versions of a conflict.
     *
     * @param base   the version both started from
     * @param yours  your version
     * @param theirs their version
     * @return what the comparison found, with the three versions open until it is closed
     * @throws IOException when a version cannot be read as a workbook
     */
    public static ConflictComparison compare(File base, File yours, File theirs) throws IOException {
        Workbook baseBook = null;
        Workbook yourBook = null;
        Workbook theirBook = null;
        try {
            baseBook = XlsWorkbookComparator.open(base);
            yourBook = XlsWorkbookComparator.open(yours);
            theirBook = XlsWorkbookComparator.open(theirs);
            var sheets = join(XlsWorkbookComparator.sheets(baseBook, yourBook),
                    XlsWorkbookComparator.sheets(baseBook, theirBook),
                    yourBook,
                    theirBook);
            return new ConflictComparison(sheets, baseBook, yourBook, theirBook);
        } catch (IOException | RuntimeException e) {
            IOUtils.closeQuietly(baseBook);
            IOUtils.closeQuietly(yourBook);
            IOUtils.closeQuietly(theirBook);
            throw e;
        }
    }

    /** The sheets of both comparisons joined by name: your order first, then the sheets only their version holds. */
    private static List<ConflictSheet> join(List<SheetComparison> yours,
                                            List<SheetComparison> theirs,
                                            Workbook yourBook,
                                            Workbook theirBook) {
        var theirSheets = new HashMap<String, SheetComparison>();
        theirs.forEach(sheet -> theirSheets.put(sheet.name(), sheet));
        var sheets = new ArrayList<ConflictSheet>();
        for (var sheet : yours) {
            var their = theirSheets.remove(sheet.name());
            sheets.add(joinSheet(sheet, their, yourBook, theirBook));
        }
        for (var their : theirs) {
            if (theirSheets.containsKey(their.name())) {
                sheets.add(joinSheet(null, their, yourBook, theirBook));
            }
        }
        return sheets;
    }

    /** A sheet, joined from what each version made of it; a version without the sheet holds none of its tables. */
    private static ConflictSheet joinSheet(@Nullable SheetComparison yours,
                                           @Nullable SheetComparison theirs,
                                           Workbook yourBook,
                                           Workbook theirBook) {
        var name = yours != null ? yours.name() : theirs.name();
        var yourTables = yours == null ? List.<TableComparison>of() : yours.tables();
        var theirTables = new ArrayList<>(theirs == null ? List.<TableComparison>of() : theirs.tables());
        var tables = new ArrayList<ConflictTable>();
        for (var table : yourTables) {
            var base = table.first();
            var their = base == null ? null : takeByBase(theirTables, base);
            tables.add(joinTable(table, their, yourBook.getSheet(name), theirBook.getSheet(name)));
        }
        for (var their : theirTables) {
            tables.add(joinTable(null, their, null, theirBook.getSheet(name)));
        }
        return new ConflictSheet(name,
                yours == null ? ChangeStatus.EQUAL : yours.status(),
                theirs == null ? ChangeStatus.EQUAL : theirs.status(),
                tables);
    }

    /** Takes the table of their version that stands for the table of the base at the given place. */
    private static @Nullable TableComparison takeByBase(List<TableComparison> tables, CellRangeAddress base) {
        for (var i = 0; i < tables.size(); i++) {
            if (base.equals(tables.get(i).first())) {
                return tables.remove(i);
            }
        }
        return null;
    }

    /**
     * A table, joined from what each version made of it. A table of the base a version does not list is equal in
     * it; a table a version added has no lines to join.
     */
    private static ConflictTable joinTable(@Nullable TableComparison yours,
                                           @Nullable TableComparison theirs,
                                           @Nullable Sheet yourSheet,
                                           @Nullable Sheet theirSheet) {
        var any = yours != null ? yours : theirs;
        var base = any.first();
        var yourStatus = statusOf(yours, base);
        var theirStatus = statusOf(theirs, base);
        var rows = base == null ? List.<ConflictLine>of()
                : lines(linesOf(yours, base, true), linesOf(theirs, base, true),
                        alikeOf(yours, yourSheet, theirs, theirSheet, false));
        var columns = base == null ? List.<ConflictLine>of()
                : lines(linesOf(yours, base, false), linesOf(theirs, base, false),
                        alikeOf(yours, yourSheet, theirs, theirSheet, true));
        var yourChanged = yours == null ? List.<CellAddress>of() : yours.secondChanged();
        var theirChanged = theirs == null ? List.<CellAddress>of() : theirs.secondChanged();
        var conflicts = conflicts(rows, columns, yours, theirs, yourSheet, theirSheet);
        return new ConflictTable(nameOf(yours, theirs, any),
                base,
                yours == null ? null : yours.second(),
                theirs == null ? null : theirs.second(),
                yourStatus,
                theirStatus,
                rows,
                columns,
                yourChanged,
                theirChanged,
                conflicts.getFirst(),
                conflicts.getLast());
    }

    /** What a version did to a table: nothing to a table of the base it does not list. */
    private static ChangeStatus statusOf(@Nullable TableComparison table, @Nullable CellRangeAddress base) {
        if (table == null) {
            return base == null ? ChangeStatus.EQUAL : ChangeStatus.REMOVED;
        }
        return table.status();
    }

    /**
     * The header of a table as your version holds it. A table your version does not hold is named as their version
     * holds it, or as the base does when neither version holds it.
     *
     * @param any the comparison of either version, yours when there is one
     */
    private static String nameOf(@Nullable TableComparison yours,
                                 @Nullable TableComparison theirs,
                                 TableComparison any) {
        return theirs != null && holds(theirs) && !holds(yours) ? theirs.name() : any.name();
    }

    /** Whether a version holds the table its comparison with the base found. */
    private static boolean holds(@Nullable TableComparison table) {
        return table != null && table.status() != ChangeStatus.REMOVED;
    }

    /**
     * The lines of the base a version holds, lined up as its comparison with the base found them. A version that
     * removed the table holds none of them.
     */
    private static List<LineChange> linesOf(@Nullable TableComparison table, CellRangeAddress base, boolean rows) {
        var count = rows
                ? base.getLastRow() - base.getFirstRow() + 1
                : base.getLastColumn() - base.getFirstColumn() + 1;
        if (table == null || table.status() == ChangeStatus.REMOVED) {
            var removed = new ArrayList<LineChange>();
            for (var line = 0; line < count; line++) {
                removed.add(new LineChange(line, LineChange.NONE, ChangeStatus.REMOVED));
            }
            return removed;
        }
        return rows ? table.rows() : table.columns();
    }

    /** Tells whether a line of your version and a line of theirs hold the same values. */
    @FunctionalInterface
    interface LinesAlike {
        boolean alike(int yours, int theirs);
    }

    /**
     * The lines of the two versions joined by the lines of the base. Before each line of the base stand the lines
     * your version inserted there, then the ones their version did; the lines inserted after the last line of the
     * base end the table. The lines both versions inserted alike at one place are one line, the lines each inserted
     * alone standing around it in their order.
     *
     * @param alike tells whether a line of your version and a line of theirs hold the same values
     */
    static List<ConflictLine> lines(List<LineChange> yours, List<LineChange> theirs, LinesAlike alike) {
        var yourLines = Placed.of(yours);
        var theirLines = Placed.of(theirs);
        var count = Math.max(yourLines.atBase().size(), theirLines.atBase().size());
        var lines = new ArrayList<ConflictLine>();
        for (var base = 0; base <= count; base++) {
            lines.addAll(inserted(yourLines.insertedBefore(base), theirLines.insertedBefore(base), alike));
            if (base < count) {
                var your = yourLines.at(base);
                var their = theirLines.at(base);
                lines.add(new ConflictLine(base, your.second(), their.second(), your.status(), their.status()));
            }
        }
        return lines;
    }

    /**
     * The lines both versions inserted at one place of the base. The most lines inserted alike are paired, in order;
     * the lines each version inserted alone stand before them, yours first. Runs too long to pair stand apart.
     */
    private static List<ConflictLine> inserted(List<LineChange> yours, List<LineChange> theirs, LinesAlike alike) {
        var best = alikeCounts(yours, theirs, alike);
        var lines = new ArrayList<ConflictLine>();
        var i = 0;
        var j = 0;
        while (i < yours.size() || j < theirs.size()) {
            if (pairedAt(best, yours, theirs, alike, i, j)) {
                lines.add(new ConflictLine(LineChange.NONE, yours.get(i++).second(), theirs.get(j++).second(),
                        ChangeStatus.ADDED, ChangeStatus.ADDED));
            } else if (yoursNext(best, yours.size(), theirs.size(), i, j)) {
                lines.add(new ConflictLine(LineChange.NONE, yours.get(i++).second(), LineChange.NONE,
                        ChangeStatus.ADDED, ChangeStatus.EQUAL));
            } else {
                lines.add(new ConflictLine(LineChange.NONE, LineChange.NONE, theirs.get(j++).second(),
                        ChangeStatus.EQUAL, ChangeStatus.ADDED));
            }
        }
        return lines;
    }

    /**
     * How many alike lines the lines of yours from each place and of theirs from each place hold: {@code best[i][j]}
     * for the lines from {@code i} and from {@code j}. Runs too long to pair hold none.
     */
    private static int[][] alikeCounts(List<LineChange> yours, List<LineChange> theirs, LinesAlike alike) {
        var k = yours.size();
        var m = theirs.size();
        var best = new int[k + 1][m + 1];
        if ((long) k * m > MAX_PAIRED) {
            return best;
        }
        for (var i = k - 1; i >= 0; i--) {
            for (var j = m - 1; j >= 0; j--) {
                best[i][j] = alikeAt(yours, theirs, alike, i, j)
                        ? best[i + 1][j + 1] + 1
                        : Math.max(best[i + 1][j], best[i][j + 1]);
            }
        }
        return best;
    }

    /** Whether the next lines of both are one line: alike, and among the most alike lines paired. */
    private static boolean pairedAt(int[][] best,
                                    List<LineChange> yours,
                                    List<LineChange> theirs,
                                    LinesAlike alike,
                                    int i,
                                    int j) {
        var both = i < yours.size() && j < theirs.size();
        return both && best[i][j] > 0 && best[i][j] == best[i + 1][j + 1] + 1 && alikeAt(yours, theirs, alike, i, j);
    }

    /** Whether your next line stands before theirs: theirs are over, or leaving yours out keeps as many pairs. */
    private static boolean yoursNext(int[][] best, int k, int m, int i, int j) {
        return i < k && (j == m || best[i + 1][j] >= best[i][j + 1]);
    }

    private static boolean alikeAt(List<LineChange> yours, List<LineChange> theirs, LinesAlike alike, int i, int j) {
        return alike.alike(yours.get(i).second(), theirs.get(j).second());
    }

    /**
     * Tells whether a line of your version of a table and a line of theirs hold the same values, as the cells of the
     * line show them. Versions that do not both hold the table share no line.
     *
     * @param columns whether the lines are columns rather than rows
     */
    private static LinesAlike alikeOf(@Nullable TableComparison yours,
                                      @Nullable Sheet yourSheet,
                                      @Nullable TableComparison theirs,
                                      @Nullable Sheet theirSheet,
                                      boolean columns) {
        var yourTable = yours == null ? null : yours.second();
        var theirTable = theirs == null ? null : theirs.second();
        if (yourTable == null || theirTable == null || yourSheet == null || theirSheet == null) {
            return (your, their) -> false;
        }
        var yourLines = digestsOf(yourSheet, yourTable, columns);
        var theirLines = digestsOf(theirSheet, theirTable, columns);
        return (your, their) -> your < yourLines.length && their < theirLines.length
                && Arrays.equals(yourLines[your], theirLines[their]);
    }

    private static long[][] digestsOf(Sheet sheet, CellRangeAddress table, boolean columns) {
        return new XlsTableMerger.View(new Cursor(sheet.getWorkbook(), sheet), table, columns).shownDigests();
    }

    /**
     * The lines of one version by the line of the base each stands for, and the lines it inserted by the line of the
     * base they stand before.
     */
    private record Placed(Map<Integer, LineChange> atBase, Map<Integer, List<LineChange>> inserted) {

        static Placed of(List<LineChange> lines) {
            var atBase = new HashMap<Integer, LineChange>();
            var inserted = new HashMap<Integer, List<LineChange>>();
            var pending = new ArrayList<LineChange>();
            for (var line : lines) {
                if (line.first() == LineChange.NONE) {
                    pending.add(line);
                } else {
                    atBase.put(line.first(), line);
                    if (!pending.isEmpty()) {
                        inserted.put(line.first(), List.copyOf(pending));
                        pending.clear();
                    }
                }
            }
            inserted.put(Integer.MAX_VALUE, List.copyOf(pending));
            return new Placed(atBase, inserted);
        }

        /** The line the version holds for a line of the base, equal when its comparison did not line it up. */
        LineChange at(int base) {
            return atBase.getOrDefault(base, new LineChange(base, base, ChangeStatus.EQUAL));
        }

        /** The lines inserted before a line of the base; before the line after the last, the lines that end it. */
        List<LineChange> insertedBefore(int base) {
            var before = inserted.get(base >= atBase.size() ? Integer.MAX_VALUE : base);
            return before == null ? List.of() : before;
        }
    }

    /**
     * The cells both versions changed differently, in your version and in theirs: a cell of a line both hold, whose
     * row and column both versions changed, and whose values differ.
     */
    private static List<List<CellAddress>> conflicts(List<ConflictLine> rows,
                                                     List<ConflictLine> columns,
                                                     @Nullable TableComparison yours,
                                                     @Nullable TableComparison theirs,
                                                     @Nullable Sheet yourSheet,
                                                     @Nullable Sheet theirSheet) {
        if (yours == null || theirs == null || yourSheet == null || theirSheet == null
                || yours.second() == null || theirs.second() == null) {
            return List.of(List.of(), List.of());
        }
        var yourChanged = new LinkedHashSet<>(yours.secondChanged());
        var theirChanged = new LinkedHashSet<>(theirs.secondChanged());
        var yourCorner = yours.second();
        var theirCorner = theirs.second();
        var yourConflicts = new ArrayList<CellAddress>();
        var theirConflicts = new ArrayList<CellAddress>();
        var sharedColumns = heldByAll(columns);
        for (var row : heldByAll(rows)) {
            for (var column : sharedColumns) {
                var your = new CellAddress(yourCorner.getFirstRow() + row.yours(),
                        yourCorner.getFirstColumn() + column.yours());
                var their = new CellAddress(theirCorner.getFirstRow() + row.theirs(),
                        theirCorner.getFirstColumn() + column.theirs());
                if (yourChanged.contains(your) && theirChanged.contains(their)
                        && !sameValue(yourSheet, your, theirSheet, their)) {
                    yourConflicts.add(your);
                    theirConflicts.add(their);
                }
            }
        }
        return List.of(yourConflicts, theirConflicts);
    }

    /** The lines the base and both versions hold: only their cells can be changed by both. */
    private static List<ConflictLine> heldByAll(List<ConflictLine> lines) {
        return lines.stream()
                .filter(line -> line.base() != LineChange.NONE
                        && line.yours() != LineChange.NONE
                        && line.theirs() != LineChange.NONE)
                .toList();
    }

    /** Whether two cells of two versions hold the same value or formula. */
    private static boolean sameValue(Sheet yourSheet, CellAddress your, Sheet theirSheet, CellAddress their) {
        var yourCursor = new Cursor(yourSheet.getWorkbook(), yourSheet);
        yourCursor.cell = SheetUtil.getCell(yourSheet, your.getRow(), your.getColumn());
        var theirCursor = new Cursor(theirSheet.getWorkbook(), theirSheet);
        theirCursor.cell = SheetUtil.getCell(theirSheet, their.getRow(), their.getColumn());
        return XlsSheetsMatcher.equalValues(yourCursor, theirCursor);
    }
}
