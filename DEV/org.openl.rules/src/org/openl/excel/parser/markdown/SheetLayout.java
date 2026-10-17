package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.jspecify.annotations.Nullable;

import org.openl.excel.parser.MergedCell;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.util.StringUtils;

/**
 * Lays the table of a file out as the cells an Excel workbook would give, and reads it back from them.
 *
 * <p>The table starts in the top left corner with its header cell merged across the table. Properties follow under a
 * {@code properties} cell merged down, one property per row, its name and value in the next two columns. The body
 * comes last. A body narrower than the properties has the last cell of each row merged over the columns they take, as
 * a workbook lays it out.
 *
 * <p>Reading back accepts only what the file can hold. The merges of the header and of the {@code properties} cell
 * follow from the layout, so they are not written; a property value merged to the right is read as a plain value.
 * Anything else above the body, or a merged range reaching into the body from above, is refused.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SheetLayout {

    private static final int PROPERTIES_WIDTH = 3;

    /**
     * Returns the cells of a sheet holding the table.
     *
     * <p>All rows have the same length. Merged cells are marked as {@link MergedCell} values.
     */
    static @Nullable Object[][] layOut(RulesTable table) {
        var sheet = cellsOf(table);
        // Copied into rows of objects, which hold the merge marks a row of strings cannot
        var cells = Arrays.stream(sheet.values())
                .map(row -> Arrays.copyOf(row, row.length, Object[].class))
                .toArray(Object[][]::new);
        sheet.merges().forEach(range -> mark(cells, range));
        return cells;
    }

    /**
     * Returns the row of the sheet the body of a table with these properties starts in.
     */
    static int bodyTop(Map<String, String> properties) {
        return 1 + properties.size();
    }

    /**
     * Returns the values and the merged ranges of a sheet holding the table.
     */
    static SheetCells cellsOf(RulesTable table) {
        var properties = table.properties();
        // A table with a header alone is a column wide
        var width = Math.max(table.width(), properties.isEmpty() ? 1 : PROPERTIES_WIDTH);
        var bodyTop = bodyTop(properties);
        var values = new @Nullable String[bodyTop + table.body().size()][width];
        var merges = new ArrayList<CellRangeAddress>();

        values[0][0] = table.header();
        addMerge(merges, new CellRangeAddress(0, 0, 0, width - 1));
        if (!properties.isEmpty()) {
            values[1][0] = PropertiesHelper.PROPERTIES_HEADER;
            addMerge(merges, new CellRangeAddress(1, properties.size(), 0, 0));
        }
        var row = 1;
        for (var property : properties.entrySet()) {
            values[row][1] = property.getKey();
            values[row][2] = property.getValue();
            row++;
        }
        for (var cells : table.body()) {
            var target = values[row++];
            for (var column = 0; column < cells.size(); column++) {
                target[column] = cells.get(column);
            }
        }
        merges.addAll(bodyMerges(table, bodyTop, width));
        return new SheetCells(values, merges);
    }

    /**
     * Returns the merged ranges of the body on the sheet. The last cell of each row reaches the last column of the
     * table.
     */
    private static List<CellRangeAddress> bodyMerges(RulesTable table, int bodyTop, int width) {
        var last = table.width() - 1;
        var merges = new ArrayList<CellRangeAddress>();
        var reaching = new BitSet();
        for (var range : table.merges()) {
            var widened = range.getLastColumn() == last;
            if (widened) {
                reaching.set(range.getFirstRow(), range.getLastRow() + 1);
            }
            addMerge(merges, new CellRangeAddress(range.getFirstRow() + bodyTop,
                    range.getLastRow() + bodyTop,
                    range.getFirstColumn(),
                    widened ? width - 1 : range.getLastColumn()));
        }
        for (var row = reaching.nextClearBit(0); row < table.body().size(); row = reaching.nextClearBit(row + 1)) {
            addMerge(merges, new CellRangeAddress(row + bodyTop, row + bodyTop, last, width - 1));
        }
        return merges;
    }

    /**
     * Reads the table back from the cells of a sheet, the table starting in the top left corner.
     *
     * @throws MarkdownWriteException when the cells hold something a Markdown file cannot
     */
    static RulesTable tableOf(SheetCells sheet) {
        var header = valueOf(sheet, 0, 0);
        if (header == null) {
            throw refuse("The first cell of the table must hold its header");
        }
        var headerMerge = sheet.mergeAt(0, 0);
        if (headerMerge != null && headerMerge.getLastRow() > 0) {
            throw refuse("The header of the table takes one row");
        }
        var properties = propertiesOf(sheet);
        var bodyTop = bodyTop(properties);
        checkAboveBody(sheet, bodyTop, headerMerge);

        var height = Math.max(extent(sheet, true), bodyTop);
        var width = extent(sheet, false);
        if (!properties.isEmpty() && width < PROPERTIES_WIDTH) {
            // A workbook does not read such properties; a file would give each an empty value
            throw refuse("A property needs the third column for its value, but the table is %d columns wide"
                    .formatted(width));
        }
        var bodyWidth = properties.isEmpty() || width > PROPERTIES_WIDTH ? width : widthUnderProperties(sheet,
                bodyTop,
                height);
        var body = IntStream.range(bodyTop, height).mapToObj(row -> bodyRow(sheet, row, bodyWidth)).toList();
        // The last cells of a narrower body merged over the columns of the properties follow from the layout
        var merges = sheet.merges()
                .stream()
                .filter(range -> range.getFirstRow() >= bodyTop)
                .map(range -> new CellRangeAddress(range.getFirstRow() - bodyTop,
                        range.getLastRow() - bodyTop,
                        range.getFirstColumn(),
                        Math.min(range.getLastColumn(), bodyWidth - 1)))
                .filter(range -> range.getNumberOfCells() > 1)
                .toList();
        return new RulesTable(header.strip(), properties, body, merges);
    }

    private static SequencedMap<String, String> propertiesOf(SheetCells sheet) {
        var properties = new LinkedHashMap<String, String>();
        var marker = valueOf(sheet, 1, 0);
        if (marker == null || !PropertiesHelper.PROPERTIES_HEADER.equals(marker.strip())) {
            return properties;
        }
        var range = sheet.mergeAt(1, 0);
        if (range != null && (range.getFirstRow() != 1 || range.getLastColumn() > 0)) {
            throw refuse("The properties cell takes one column");
        }
        var last = range == null ? 1 : range.getLastRow();
        for (var row = 1; row <= last; row++) {
            var name = valueOf(sheet, row, 1);
            if (name == null || isMerged(sheet, row, 1)) {
                throw refuse("The property in row %d must have a name in a cell of its own".formatted(row + 1));
            }
            var value = valueOf(sheet, row, 2);
            name = name.strip();
            if (properties.put(name, value == null ? "" : value) != null || isReserved(name)) {
                throw refuse("The property '%s' cannot be written: the name is taken".formatted(name));
            }
        }
        return properties;
    }

    private static boolean isReserved(String name) {
        return "table".equals(name) || "merges".equals(name);
    }

    /**
     * Refuses values and merged ranges above the body that the layout does not give.
     */
    private static void checkAboveBody(SheetCells sheet, int bodyTop, @Nullable CellRangeAddress headerMerge) {
        for (var row = 0; row < Math.min(bodyTop, sheet.values().length); row++) {
            for (var column = 0; column < sheet.values()[row].length; column++) {
                if (valueOf(sheet, row, column) != null && !isLaidOut(row, column)) {
                    throw refuse("Only the header and the properties stand above the body, but %s holds a value"
                            .formatted(cellName(row, column)));
                }
            }
        }
        for (var range : sheet.merges()) {
            if (range.getFirstRow() < bodyTop && !isLaidOut(range, bodyTop, headerMerge)) {
                throw refuse("The merged range %s cannot be written in a Markdown file"
                        .formatted(range.formatAsString()));
            }
        }
    }

    private static boolean isLaidOut(int row, int column) {
        return row == 0 ? column == 0 : column < PROPERTIES_WIDTH && (column > 0 || row == 1);
    }

    private static boolean isLaidOut(CellRangeAddress range, int bodyTop, @Nullable CellRangeAddress headerMerge) {
        var propertyValue = range.getFirstRow() > 0 && range.getLastRow() == range.getFirstRow()
                && range.getFirstColumn() == 2;
        var marker = range.getFirstRow() == 1 && range.getFirstColumn() == 0 && range.getLastColumn() == 0;
        return range.getLastRow() < bodyTop && (range.equals(headerMerge) || propertyValue || marker);
    }

    private static List<@Nullable String> bodyRow(SheetCells sheet, int row, int width) {
        var cells = new ArrayList<@Nullable String>(width);
        for (var column = 0; column < width; column++) {
            var value = valueOf(sheet, row, column);
            var range = sheet.mergeAt(row, column);
            if (value != null && range != null && (range.getFirstRow() != row || range.getFirstColumn() != column)) {
                throw refuse("The cell %s has a value, but the merged range %s covers it".formatted(
                        cellName(row, column), range.formatAsString()));
            }
            cells.add(value);
        }
        return cells;
    }

    /**
     * Returns the number of rows, or of columns, up to the last one holding a value or a merged range.
     */
    private static int extent(SheetCells sheet, boolean rows) {
        var extent = 0;
        for (var row = 0; row < sheet.values().length; row++) {
            for (var column = 0; column < sheet.values()[row].length; column++) {
                if (valueOf(sheet, row, column) != null) {
                    extent = Math.max(extent, (rows ? row : column) + 1);
                }
            }
        }
        for (var range : sheet.merges()) {
            extent = Math.max(extent, (rows ? range.getLastRow() : range.getLastColumn()) + 1);
        }
        return extent;
    }

    /**
     * Returns the width of a body under properties, which widen the table to three columns. A narrower body keeps its
     * own width when the last cell of each row is merged over the columns of the properties, as the layout gives it;
     * else the body takes all three.
     */
    private static int widthUnderProperties(SheetCells sheet, int bodyTop, int height) {
        var own = bodyWidth(sheet, bodyTop);
        return IntStream.range(bodyTop, height).allMatch(row -> reachesTheProperties(sheet, row, own - 1))
                ? own
                : PROPERTIES_WIDTH;
    }

    private static boolean reachesTheProperties(SheetCells sheet, int row, int column) {
        var range = sheet.mergeAt(row, column);
        return (range == null ? column : range.getLastColumn()) == PROPERTIES_WIDTH - 1;
    }

    private static int bodyWidth(SheetCells sheet, int bodyTop) {
        var width = 1;
        for (var row = bodyTop; row < sheet.values().length; row++) {
            for (var column = 0; column < sheet.values()[row].length; column++) {
                if (valueOf(sheet, row, column) != null) {
                    width = Math.max(width, column + 1);
                }
            }
        }
        for (var range : sheet.merges()) {
            if (range.getFirstRow() >= bodyTop) {
                width = Math.max(width, range.getFirstColumn() + 1);
            }
        }
        return width;
    }

    private static @Nullable String valueOf(SheetCells sheet, int row, int column) {
        var value = sheet.value(row, column);
        return StringUtils.isBlank(value) ? null : value;
    }

    private static boolean isMerged(SheetCells sheet, int row, int column) {
        var range = sheet.mergeAt(row, column);
        return range != null && range.getNumberOfCells() > 1;
    }

    private static void addMerge(List<CellRangeAddress> merges, CellRangeAddress range) {
        if (range.getNumberOfCells() > 1) {
            merges.add(range);
        }
    }

    private static void mark(@Nullable Object[][] cells, CellRangeAddress range) {
        for (var row = range.getFirstRow(); row <= range.getLastRow(); row++) {
            var target = cells[row];
            Arrays.fill(target, range.getFirstColumn() + 1, range.getLastColumn() + 1, MergedCell.MERGE_WITH_LEFT);
            if (row > range.getFirstRow()) {
                target[range.getFirstColumn()] = MergedCell.MERGE_WITH_UP;
            }
        }
    }

    private static String cellName(int row, int column) {
        return new CellReference(row, column).formatAsString();
    }

    private static MarkdownWriteException refuse(String message) {
        return new MarkdownWriteException(message);
    }
}
