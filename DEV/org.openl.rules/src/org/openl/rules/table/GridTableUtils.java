package org.openl.rules.table;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @author Andrei Ostrovski, Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridTableUtils {

    /**
     * Extracts original table.
     *
     * @param table Table.
     * @return Original table if table is decorator and current table otherwise.
     */
    public static IGridTable getOriginalTable(IGridTable table) {
        var resultTable = table;

        while (resultTable instanceof AGridTableDecorator decorator) {
            resultTable = decorator.getOriginalGridTable();
        }

        return resultTable;
    }

    /**
     * Returns all regions of a table: the region of each cell of the sheet its cells cover.
     *
     * <p>A cell of the table that covers several cells of the sheet, such as the values a rule returns as one array,
     * gives the region of each of them. A merged region is given once.
     *
     * @param table the table with regions.
     * @return a the regions of the table, row by row.
     */
    public static @NonNull List<@NonNull IGridRegion> getGridRegions(@NonNull ILogicalTable table) {
        var regions = new ArrayList<IGridRegion>();
        var top = 0;
        for (var row = 0; row < table.getHeight(); row++) {
            var height = table.getRowHeight(row);
            var left = 0;
            for (var column = 0; column < table.getWidth(); column++) {
                var width = table.getColumnWidth(column);
                addBlockRegions(regions, table.getSource(), left, top, width, height);
                left += width;
            }
            top += height;
        }
        return regions;
    }

    /**
     * Adds the regions of the cells of a block of the grid that no region added before covers.
     *
     * <p>A region merged across the edge of the block is added by the first block it reaches only.
     */
    private static void addBlockRegions(@NonNull List<@NonNull IGridRegion> regions,
                                        @NonNull IGridTable grid,
                                        int left,
                                        int top,
                                        int width,
                                        int height) {
        for (var row = top; row < top + height; row++) {
            for (var column = left; column < left + width; column++) {
                var cell = grid.getCell(column, row);
                if (!covers(regions, cell.getAbsoluteColumn(), cell.getAbsoluteRow())) {
                    regions.add(cell.getAbsoluteRegion());
                }
            }
        }
    }

    private static boolean covers(@NonNull List<@NonNull IGridRegion> regions, int column, int row) {
        for (var region : regions) {
            if (GridRegionUtils.contains(region, column, row)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the two cells belong to one merged region. */
    public static boolean isSameMergedCell(@NonNull ICell cell, @NonNull ICell other) {
        var region = cell.getRegion();
        var otherRegion = other.getRegion();
        return region != null && otherRegion != null
                && region.getTop() == otherRegion.getTop()
                && region.getLeft() == otherRegion.getLeft();
    }

    public static boolean isSingleCellTable(ILogicalTable table) {
        return table.getHeight() == 1 && table.getWidth() == 1;
    }

    /**
     * Whether a table is assembled from parts written apart from one another, such as on several sheets.
     *
     * <p>Such a table stands on no sheet of its own, so nothing can be written into it.
     *
     * @param table the grid the table stands on, or {@code null} for a table that has none
     * @return {@code true} if the table is assembled from parts, {@code false} otherwise
     */
    public static boolean isAssembledFromParts(@Nullable IGridTable table) {
        return table != null && table.getGrid() instanceof CompositeGrid;
    }
}
