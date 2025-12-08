package org.openl.studio.projects.service.tables.write;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableAppend;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;

/**
 * Writes {@link RawTableView} back to the original table preserving the exact 2D matrix structure.
 * <p>
 * This writer directly processes the raw 2D matrix from RawTableView:
 * <ul>
 *   <li>Iterates through all cells in the source matrix (row 0 onwards)
 *   <li>Writes each non-covered cell value to the grid
 *   <li>Skips covered cells (marked with {@code covered=true})
 *   <li>Cleans up removed rows automatically
 * </ul>
 * <p>
 * The source matrix is written directly without interpretation:
 * <ul>
 *   <li>Cell values are written as-is (Object type)
 *   <li>Merge information (colspan/rowspan) is implicit in the matrix structure
 *   <li>No type validation or schema interpretation is performed
 *   <li>Works with any table type (Data, Test, Spreadsheet, etc.)
 * </ul>
 * <p>
 * Note: Unlike typed table writers (DataTableWriter, TestTableWriter) which have separate
 * header and business body sections, RawTableWriter treats the entire source matrix uniformly,
 * writing all rows (including headers) to the table's developer view.
 *
 * @author Vladyslav Pikus
 */
public class RawTableWriter extends TableWriter<RawTableView> {

    public RawTableWriter(IOpenLTable table) {
        super(table);
    }

    public RawTableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    /**
     * No special header processing for raw tables.
     * <p>
     * Raw tables have the header as part of the source matrix at row 0, which is processed
     * together with the rest of the matrix in {@link #updateBusinessBody(RawTableView)}.
     * The TableWriter base class handles common header operations, but raw format
     * doesn't need table-specific header interpretation.
     *
     * @param tableView The raw table view (header already included in source matrix)
     */
    @Override
    protected void updateHeader(RawTableView tableView) {
        // No header update needed for raw tables - it's part of the source matrix
    }

    /**
     * Write the entire source matrix to the table's developer view.
     * <p>
     * Processing (two-phase):
     * <ul>
     *   <li><b>Phase 1 (Write values):</b>
     *     <ul>
     *       <li>Iterates through all rows in the source matrix (starting from row 0)
     *       <li>For each row, iterates through all cells in that row
     *       <li>Skips null cells and cells marked with {@code covered=true}
     *       <li>Writes non-covered cell values directly to the grid
     *       <li>Tracks merge regions (cells with colspan > 1 or rowspan > 1)
     *       <li>Cleans up extra rows beyond the source matrix size
     *     </ul>
     *   <li><b>Phase 2 (Apply merges):</b>
     *     <ul>
     *       <li>After all values are written, applies merge regions
     *       <li>This ensures columns/rows are created before merging
     *       <li>Uses MergeCellsAction for each merge region
     *     </ul>
     * </ul>
     * <p>
     * Two-phase approach prevents issues where merging before all cells are written
     * could cause grid expansion to fail or lose merge information.
     *
     * @param tableView The raw table view containing the 2D matrix of cells
     */
    @Override
    protected void updateBusinessBody(RawTableView tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_DEVELOPER);
        int maxSourceRow = tableView.source.size();
        List<IGridRegion> mergeRegions = new ArrayList<>();

        // Phase 1: Write all cell values and track merge regions
        for (int row = 0; row < maxSourceRow; row++) {
            List<RawTableCell> rowCells = tableView.source.get(row);
            for (int col = 0; col < rowCells.size(); col++) {
                RawTableCell cell = rowCells.get(col);
                if (cell == null || Boolean.TRUE.equals(cell.covered())) {
                    // Covered cells are skipped
                    continue;
                }
                // Write origin cell
                Object value = cell.value();
                createOrUpdateCell(tableBody, buildCellKey(col, row), value);

                buildMergeRegionIfNeeded(cell, row, col)
                        .ifPresent(mergeRegions::add);
            }
        }

        if (isUpdateMode()) {
            // Clean up removed rows
            var height = Tool.height(tableBody.getRegion());
            if (maxSourceRow < height) {
                removeRows(tableBody, height - maxSourceRow, maxSourceRow);
            }
        }

        // Phase 2: Apply merge regions after all cells are written
        applyMergeRegions(tableBody, mergeRegions);
    }

    private Optional<GridRegion> buildMergeRegionIfNeeded(RawTableCell cell, int row, int col) {
        int colspan = cell.colspan() != null ? cell.colspan() : 1;
        int rowspan = cell.rowspan() != null ? cell.rowspan() : 1;
        if (colspan > 1 || rowspan > 1) {
            return Optional.of(new GridRegion(row, col, row + rowspan - 1, col + colspan - 1));
        }
        return Optional.empty();
    }

    /**
     * Append new rows to the end of the table.
     * <p>
     * Appends rows in raw matrix format to the table's developer view without
     * affecting existing content. Covered cells are skipped (they are masked by
     * their origin cell's span). Also applies merge regions for appended cells.
     *
     * @param tableAppend The append request containing rows to add
     */
    public void append(RawTableAppend tableAppend) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);
            int startRow = Tool.height(tableBody.getRegion());
            List<IGridRegion> mergeRegions = new ArrayList<>();

            // Write rows and track merge regions
            int currentRow = startRow;
            for (var row : tableAppend.getRows()) {
                int col = 0;
                for (var cell : row) {
                    if (cell == null || Boolean.TRUE.equals(cell.covered())) {
                        // Covered cells are skipped
                        col++;
                        continue;
                    }
                    Object value = cell.value();
                    createOrUpdateCell(tableBody, buildCellKey(col, currentRow), value);

                    // Track merge region if cell has span
                    buildMergeRegionIfNeeded(cell, currentRow, col)
                            .ifPresent(mergeRegions::add);
                    col++;
                }
                currentRow++;
            }

            // Apply merge regions after all cells are written
            applyMergeRegions(tableBody, mergeRegions);
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

}
