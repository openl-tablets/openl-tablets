package org.openl.rules.xls.merge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeUtil;
import org.apache.poi.ss.util.SheetUtil;
import org.jspecify.annotations.NullMarked;

/**
 * Finds the tables of a sheet and pairs the tables of two revisions of it.
 *
 * <p>A table is a block of cells with something in them: a value, a style, a comment or a merged cell. It ends at a
 * fully empty row or column, the way OpenL reads a sheet, so cells that touch each other, the diagonal included,
 * belong to one table.
 */
@NullMarked
final class XlsSheetTables {

    private XlsSheetTables() {
    }

    /**
     * Finds the tables of a sheet, from top to bottom and from left to right.
     */
    static List<CellRangeAddress> split(Sheet sheet) {
        var tables = new ArrayList<CellRangeAddress>();
        for (Row row : sheet) {
            var runStart = -1;
            var previous = -2;
            for (Cell cell : row) {
                if (!XlsSheetsMatcher.isBlankAndUnstyled(cell)) {
                    var column = cell.getColumnIndex();
                    if (column != previous + 1) {
                        addRun(tables, row.getRowNum(), runStart, previous);
                        runStart = column;
                    }
                    previous = column;
                }
            }
            addRun(tables, row.getRowNum(), runStart, previous);
        }
        tables.addAll(sheet.getMergedRegions());
        return join(tables);
    }

    private static void addRun(List<CellRangeAddress> tables, int row, int first, int last) {
        if (first >= 0) {
            tables.add(new CellRangeAddress(row, row, first, last));
        }
    }

    /**
     * Joins the blocks that touch each other until every two blocks have an empty row or column between them.
     */
    private static List<CellRangeAddress> join(List<CellRangeAddress> blocks) {
        var tables = new ArrayList<CellRangeAddress>();
        for (CellRangeAddress block : blocks) {
            var joined = block;
            var touched = true;
            while (touched) {
                touched = false;
                for (var i = tables.size() - 1; i >= 0; i--) {
                    if (touch(joined, tables.get(i))) {
                        joined = CellRangeUtil.createEnclosingCellRange(joined, tables.remove(i));
                        touched = true;
                    }
                }
            }
            tables.add(joined);
        }
        tables.sort(Comparator.comparingInt(CellRangeAddress::getFirstRow)
                .thenComparingInt(CellRangeAddress::getFirstColumn));
        return tables;
    }

    /**
     * Tells whether two blocks overlap or touch each other, the diagonal included.
     */
    static boolean touch(CellRangeAddress a, CellRangeAddress b) {
        return a.getFirstRow() <= b.getLastRow() + 1 && b.getFirstRow() <= a.getLastRow() + 1
                && a.getFirstColumn() <= b.getLastColumn() + 1 && b.getFirstColumn() <= a.getLastColumn() + 1;
    }

    /**
     * Pairs the tables of BASE revision with the tables of another revision.
     *
     * <p>A table is found by the text of its first cell, the header of an OpenL table: first at the same place, then
     * anywhere when the header is unique in both revisions, so a moved table is found, and last at the same place
     * whatever its header, so a table whose header changed is found.
     *
     * @return the table of the other revision by the table of BASE revision; a table of BASE revision with no pair is
     * removed in the other revision, and a table of the other revision with no pair is created in it
     */
    static Map<CellRangeAddress, CellRangeAddress> match(List<CellRangeAddress> baseTables,
                                                         Sheet baseSheet,
                                                         List<CellRangeAddress> tables,
                                                         Sheet sheet) {
        var baseHeaders = headers(baseSheet, baseTables);
        var headers = headers(sheet, tables);
        var pairs = new HashMap<CellRangeAddress, CellRangeAddress>();
        var unpairedBase = new ArrayList<>(baseTables);
        var unpaired = new ArrayList<>(tables);
        pair(unpairedBase, unpaired, pairs, (base, table) -> sameStart(base, table)
                && baseHeaders.get(base).equals(headers.get(table)));
        pair(unpairedBase, unpaired, pairs, (base, table) -> {
            var text = baseHeaders.get(base);
            return !text.isEmpty() && text.equals(headers.get(table))
                    && count(unpairedBase, baseHeaders, text) == 1
                    && count(unpaired, headers, text) == 1;
        });
        pair(unpairedBase, unpaired, pairs, XlsSheetTables::sameStart);
        return pairs;
    }

    private static void pair(List<CellRangeAddress> unpairedBase,
                             List<CellRangeAddress> unpaired,
                             Map<CellRangeAddress, CellRangeAddress> pairs,
                             BiPredicate<CellRangeAddress, CellRangeAddress> same) {
        for (var base : List.copyOf(unpairedBase)) {
            unpaired.stream().filter(candidate -> same.test(base, candidate)).findFirst().ifPresent(table -> {
                pairs.put(base, table);
                unpairedBase.remove(base);
                unpaired.remove(table);
            });
        }
    }

    private static long count(List<CellRangeAddress> tables, Map<CellRangeAddress, String> headers, String text) {
        return tables.stream().filter(table -> text.equals(headers.get(table))).count();
    }

    /**
     * The text of the first cell of every table, or an empty text when the cell has no value.
     */
    private static Map<CellRangeAddress, String> headers(Sheet sheet, List<CellRangeAddress> tables) {
        var headers = new HashMap<CellRangeAddress, String>();
        for (CellRangeAddress table : tables) {
            var cell = SheetUtil.getCell(sheet, table.getFirstRow(), table.getFirstColumn());
            headers.put(table, XlsSheetsMatcher.hasValue(cell) ? cell.toString().trim() : "");
        }
        return headers;
    }

    private static boolean sameStart(CellRangeAddress a, CellRangeAddress b) {
        return a.getFirstRow() == b.getFirstRow() && a.getFirstColumn() == b.getFirstColumn();
    }
}
