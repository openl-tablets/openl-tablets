package org.openl.excel.parser.markdown;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

import org.openl.excel.parser.MergedCell;
import org.openl.rules.table.properties.PropertiesHelper;

/**
 * Lays the table of a file out as the cells an Excel workbook would give.
 *
 * <p>The table starts in the top left corner with its header cell merged across the table. Properties follow under a
 * {@code properties} cell merged down, one property per row, its name and value in the next two columns. The body
 * comes last.
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
        var properties = table.properties();
        var width = properties.isEmpty() ? table.width() : Math.max(table.width(), PROPERTIES_WIDTH);
        var bodyTop = 1 + properties.size();
        var cells = new @Nullable Object[bodyTop + table.body().size()][width];

        cells[0][0] = table.header();
        Arrays.fill(cells[0], 1, width, MergedCell.MERGE_WITH_LEFT);

        var row = 1;
        for (var property : properties.entrySet()) {
            cells[row][0] = row == 1 ? PropertiesHelper.PROPERTIES_HEADER : MergedCell.MERGE_WITH_UP;
            cells[row][1] = property.getKey();
            cells[row][2] = property.getValue();
            row++;
        }
        for (var values : table.body()) {
            var target = cells[row++];
            for (var column = 0; column < values.size(); column++) {
                target[column] = values.get(column);
            }
        }
        table.merges().forEach(range -> merge(cells, bodyTop, range));
        return cells;
    }

    private static void merge(@Nullable Object[][] cells, int bodyTop, CellRangeAddress range) {
        for (var row = range.getFirstRow(); row <= range.getLastRow(); row++) {
            var target = cells[bodyTop + row];
            Arrays.fill(target, range.getFirstColumn() + 1, range.getLastColumn() + 1, MergedCell.MERGE_WITH_LEFT);
            if (row > range.getFirstRow()) {
                target[range.getFirstColumn()] = MergedCell.MERGE_WITH_UP;
            }
        }
    }
}
