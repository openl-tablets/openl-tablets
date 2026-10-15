package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
 * comes last.
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
     * Returns the values and the merged ranges of a sheet holding the table.
     */
    static SheetCells cellsOf(RulesTable table) {
        var properties = table.properties();
        var width = properties.isEmpty() ? table.width() : Math.max(table.width(), PROPERTIES_WIDTH);
        var bodyTop = 1 + properties.size();
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
        table.merges().forEach(range -> addMerge(merges, new CellRangeAddress(range.getFirstRow() + bodyTop,
                range.getLastRow() + bodyTop,
                range.getFirstColumn(),
                range.getLastColumn())));
        return new SheetCells(values, merges);
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
        var bodyTop = 1 + properties.size();
        checkAboveBody(sheet, bodyTop, headerMerge);

        var height = extent(sheet, true);
        if (height <= bodyTop) {
            throw refuse("The table must have a row under its header");
        }
        var width = extent(sheet, false);
        if (!properties.isEmpty() && width < PROPERTIES_WIDTH) {
            // A workbook does not read such properties; a file would give each an empty value
            throw refuse("A property needs the third column for its value, but the table is %d columns wide"
                    .formatted(width));
        }
        // Properties widen the table to three columns, which a narrower body does not have
        var bodyWidth = properties.isEmpty() || width > PROPERTIES_WIDTH ? width : bodyWidth(sheet, bodyTop);
        var body = IntStream.range(bodyTop, height).mapToObj(row -> bodyRow(sheet, row, bodyWidth)).toList();
        var merges = sheet.merges()
                .stream()
                .filter(range -> range.getFirstRow() >= bodyTop && range.getNumberOfCells() > 1)
                .map(range -> new CellRangeAddress(range.getFirstRow() - bodyTop,
                        range.getLastRow() - bodyTop,
                        range.getFirstColumn(),
                        range.getLastColumn()))
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
                width = Math.max(width, range.getLastColumn() + 1);
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
