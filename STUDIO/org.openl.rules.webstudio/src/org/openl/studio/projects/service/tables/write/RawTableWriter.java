package org.openl.studio.projects.service.tables.write;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.actions.RemoveMergedRegionsAction;
import org.openl.rules.table.actions.UndoableInsertColumnsAction;
import org.openl.rules.table.actions.UndoableInsertRowsAction;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.AppendTarget;
import org.openl.studio.projects.model.tables.DeleteTarget;
import org.openl.studio.projects.model.tables.InsertTarget;
import org.openl.studio.projects.model.tables.MergeTarget;
import org.openl.studio.projects.model.tables.RawCellInput;
import org.openl.studio.projects.model.tables.RawTableAppend;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.UnmergeTarget;
import org.openl.studio.projects.model.tables.UpdateTarget;

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
     *       <li>On update, first clears the table's existing merges so merges removed from the source do not linger
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

                buildMergeRegionIfNeeded(cell.colspan(), cell.rowspan(), row, col)
                        .ifPresent(mergeRegions::add);
            }
        }

        if (isUpdateMode()) {
            // Clean up removed rows
            var height = Tool.height(tableBody.getRegion());
            if (maxSourceRow < height) {
                removeRows(tableBody, height - maxSourceRow, maxSourceRow);
            }
            // Drop the table's existing merges so that merges removed from the source do not linger.
            // Skipped on creation, where the table starts with no merges.
            clearMergedRegions(tableBody);
        }

        // Phase 2: Apply merge regions after all cells are written
        applyMergeRegions(tableBody, mergeRegions);
    }

    private static Optional<GridRegion> buildMergeRegionIfNeeded(Integer colspanValue, Integer rowspanValue,
                                                                 int row, int col) {
        int colspan = colspanValue != null ? colspanValue : 1;
        int rowspan = rowspanValue != null ? rowspanValue : 1;
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
        int tableColumns = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER).getWidth();
        for (var row : tableAppend.getRows()) {
            requireColumnsWithinTable(row.size(), tableColumns);
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
                    buildMergeRegionIfNeeded(cell.colspan(), cell.rowspan(), currentRow, col)
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

    /**
     * Apply a single in-place edit to the table's raw source matrix and save the result.
     * <p>
     * The concrete operation is chosen by the action type: appending, inserting or deleting a row or a column, or
     * updating one cell. All coordinates are 0-based and relative to the developer view (the full table including the
     * header row), matching the matrix returned by the raw read.
     *
     * @param action the edit to apply
     */
    public void apply(RawTableSourceAction action) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Source actions are only allowed in update mode.");
        }
        try {
            table.getGridTable().edit();
            dispatch(action);
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

    private void dispatch(RawTableSourceAction action) {
        switch (action) {
            case RawTableSourceAction.Append(var target) -> append(target);
            case RawTableSourceAction.Insert(var target) -> insert(target);
            case RawTableSourceAction.Delete(var target) -> delete(target);
            case RawTableSourceAction.Update(var target) -> update(target);
            case RawTableSourceAction.Merge(var target) -> merge(target);
            case RawTableSourceAction.Unmerge(var target) -> unmerge(target);
        }
    }

    private void append(AppendTarget target) {
        switch (target) {
            case AppendTarget.Row(var cells) -> appendRow(requireCells(cells));
            case AppendTarget.Column(var cells) -> appendColumn(requireCells(cells));
            case AppendTarget.Rows(var cells) -> appendRows(cells);
            case AppendTarget.Columns(var cells) -> appendColumns(cells);
        }
    }

    private void insert(InsertTarget target) {
        switch (target) {
            case InsertTarget.Row(var position, var cells) -> insertRow(position, requireCells(cells));
            case InsertTarget.Column(var position, var cells) -> insertColumn(position, requireCells(cells));
            case InsertTarget.Rows(var position, var cells) -> insertRows(position, cells);
            case InsertTarget.Columns(var position, var cells) -> insertColumns(position, cells);
        }
    }

    private void delete(DeleteTarget target) {
        switch (target) {
            case DeleteTarget.Row(var position) -> deleteRow(position);
            case DeleteTarget.Column(var position) -> deleteColumn(position);
            case DeleteTarget.Rows(var position, var count) -> deleteRows(position, count);
            case DeleteTarget.Columns(var position, var count) -> deleteColumns(position, count);
        }
    }

    private void update(UpdateTarget target) {
        switch (target) {
            case UpdateTarget.Row(var position, var cells) -> updateRow(position, requireCells(cells));
            case UpdateTarget.Column(var position, var cells) -> updateColumn(position, requireCells(cells));
            case UpdateTarget.Cell(var row, var column, var value) -> updateCell(row, column, value);
            case UpdateTarget.Range(var row, var column, var cells) -> updateRange(row, column, cells);
        }
    }

    private void merge(MergeTarget target) {
        switch (target) {
            case MergeTarget.Cells(var row, var column, var rowspan, var colspan) ->
                    mergeCells(row, column, rowspan, colspan);
        }
    }

    private void unmerge(UnmergeTarget target) {
        switch (target) {
            case UnmergeTarget.Cells(var row, var column) -> unmergeCells(row, column);
        }
    }

    private void appendRow(List<RawCellInput> cells) {
        var developerView = developerView();
        requireRowWidth(cells, Tool.width(developerView.getRegion()));
        // Writing past the last row grows the table by one row.
        writeRow(developerView, Tool.height(developerView.getRegion()), cells, false);
    }

    private void insertRow(int position, List<RawCellInput> cells) {
        var developerView = developerView();
        // The first row is the header; a new row goes at index 1..height (height appends to the end).
        requirePosition(position, 1, Tool.height(developerView.getRegion()));
        requireRowWidth(cells, Tool.width(developerView.getRegion()));
        // Row insertion lands the blank after the given index, so insert after the preceding row.
        insertBlankRows(developerView, position - 1);
        writeRow(developerView, position, cells, false);
    }

    private void deleteRow(int position) {
        var developerView = developerView();
        int height = Tool.height(developerView.getRegion());
        // The first row is the header; only body rows 1..height-1 may be deleted (symmetric with insertRow).
        requirePosition(position, 1, height - 1);
        requireDeletable(height);
        // Remove exactly the requested row; GridTool resizes any merged regions that span it.
        removeRows(developerView, 1, position);
    }

    private void appendColumn(List<RawCellInput> cells) {
        var developerView = developerView();
        requireColumnHeight(cells, Tool.height(developerView.getRegion()));
        // Writing past the last column grows the table by one column.
        writeColumn(developerView, Tool.width(developerView.getRegion()), cells, false);
    }

    private void insertColumn(int position, List<RawCellInput> cells) {
        var developerView = developerView();
        // The first column carries the leading labels; a new column goes at index 1..width (width appends to the end).
        requirePosition(position, 1, Tool.width(developerView.getRegion()));
        requireColumnHeight(cells, Tool.height(developerView.getRegion()));
        // Column insertion lands the blank at the given index (unlike row insertion, which lands it after the index).
        insertBlankColumns(developerView, position);
        writeColumn(developerView, position, cells, false);
    }

    private void deleteColumn(int position) {
        var developerView = developerView();
        int width = Tool.width(developerView.getRegion());
        // The first column carries the leading labels; only columns 1..width-1 may be deleted (symmetric with insert).
        requirePosition(position, 1, width - 1);
        requireDeletable(width);
        // Remove exactly the requested column; GridTool resizes any merged regions that span it.
        removeColumns(developerView, 1, position);
    }

    private void insertRows(int position, List<List<RawCellInput>> rows) {
        var developerView = developerView();
        requirePosition(position, 1, Tool.height(developerView.getRegion()));
        requireMultiLine(rows);
        int width = Tool.width(developerView.getRegion());
        for (var row : rows) {
            requireRowWidth(row, width);
        }
        // Insert one row at a time: a single multi-row grid insert at the table's top boundary corrupts the region.
        for (int i = 0; i < rows.size(); i++) {
            insertBlankRows(developerView, position - 1 + i);
            writeRow(developerView, position + i, rows.get(i), false);
        }
    }

    private void insertColumns(int position, List<List<RawCellInput>> columns) {
        var developerView = developerView();
        requirePosition(position, 1, Tool.width(developerView.getRegion()));
        requireMultiLine(columns);
        int height = Tool.height(developerView.getRegion());
        for (var column : columns) {
            requireColumnHeight(column, height);
        }
        // Insert one column at a time, mirroring the single-column path.
        for (int i = 0; i < columns.size(); i++) {
            insertBlankColumns(developerView, position + i);
            writeColumn(developerView, position + i, columns.get(i), false);
        }
    }

    private void appendRows(List<List<RawCellInput>> rows) {
        var developerView = developerView();
        requireMultiLine(rows);
        int width = Tool.width(developerView.getRegion());
        for (var row : rows) {
            requireRowWidth(row, width);
        }
        // Writing past the last row grows the table by one, so each row extends the table in turn.
        int startRow = Tool.height(developerView.getRegion());
        for (int i = 0; i < rows.size(); i++) {
            writeRow(developerView, startRow + i, rows.get(i), false);
        }
    }

    private void appendColumns(List<List<RawCellInput>> columns) {
        var developerView = developerView();
        requireMultiLine(columns);
        int height = Tool.height(developerView.getRegion());
        for (var column : columns) {
            requireColumnHeight(column, height);
        }
        int startColumn = Tool.width(developerView.getRegion());
        for (int i = 0; i < columns.size(); i++) {
            writeColumn(developerView, startColumn + i, columns.get(i), false);
        }
    }

    private void deleteRows(int position, int count) {
        var developerView = developerView();
        requireBlockCount(count);
        // The first row is the header; the block (position..position+count-1) must stay within the body.
        requirePosition(position, 1, Tool.height(developerView.getRegion()) - count);
        // Remove one row at a time at the same index: each removal shifts the rows below up into it.
        for (int i = 0; i < count; i++) {
            removeRows(developerView, 1, position);
        }
    }

    private void deleteColumns(int position, int count) {
        var developerView = developerView();
        requireBlockCount(count);
        requirePosition(position, 1, Tool.width(developerView.getRegion()) - count);
        for (int i = 0; i < count; i++) {
            removeColumns(developerView, 1, position);
        }
    }

    private void updateRow(int position, List<RawCellInput> cells) {
        var developerView = developerView();
        requirePosition(position, 0, Tool.height(developerView.getRegion()) - 1);
        requireRowWidth(cells, Tool.width(developerView.getRegion()));
        // Drop merges anchored in the row so a merge dropped from the new cells does not linger.
        clearLineMerges(developerView, position, true);
        writeRow(developerView, position, cells, true);
    }

    private void updateColumn(int position, List<RawCellInput> cells) {
        var developerView = developerView();
        requirePosition(position, 0, Tool.width(developerView.getRegion()) - 1);
        requireColumnHeight(cells, Tool.height(developerView.getRegion()));
        // Drop merges anchored in the column so a merge dropped from the new cells does not linger.
        clearLineMerges(developerView, position, false);
        writeColumn(developerView, position, cells, true);
    }

    private void updateCell(int row, int column, Object value) {
        var developerView = developerView();
        requireCellInBounds(developerView, row, column);
        if (isCoveredByMerge(developerView, row, column)) {
            throw new BadRequestException("table.action.cell.covered.message", new Object[]{row, column});
        }
        createOrUpdateCell(developerView, buildCellKey(column, row), value);
    }

    private void updateRange(int row, int column, List<List<RawCellInput>> cells) {
        if (cells == null || cells.isEmpty()) {
            throw new BadRequestException("table.action.cells.required.message");
        }
        var developerView = developerView();
        int height = Tool.height(developerView.getRegion());
        int width = Tool.width(developerView.getRegion());
        int rangeHeight = cells.size();
        int rangeWidth = requireRectangularRange(cells);
        requireRangeMultiCell(rangeHeight, rangeWidth);
        requireRangeInBounds(row, column, rangeHeight, rangeWidth, height, width);
        // Drop merges anchored in the block so a merge dropped from the new cells does not linger (as updateRow does).
        clearBlockMerges(developerView, row, column, rangeHeight, rangeWidth);
        writeBlock(developerView, row, column, cells, width, height);
    }

    private void writeBlock(IGridTable developerView, int top, int left, List<List<RawCellInput>> cells,
                            int width, int height) {
        List<IGridRegion> mergeRegions = new ArrayList<>();
        for (int r = 0; r < cells.size(); r++) {
            List<RawCellInput> rowCells = cells.get(r);
            for (int c = 0; c < rowCells.size(); c++) {
                writeCellInput(developerView, top + r, left + c, rowCells.get(c), true, width, height, mergeRegions);
            }
        }
        applyMergeRegions(developerView, mergeRegions);
    }

    private void clearBlockMerges(IGridTable developerView, int top, int left, int rangeHeight, int rangeWidth) {
        var tableRegion = developerView.getRegion();
        var blockRegion = new GridRegion(tableRegion.getTop() + top, tableRegion.getLeft() + left,
                tableRegion.getTop() + top + rangeHeight - 1, tableRegion.getLeft() + left + rangeWidth - 1);
        var action = new RemoveMergedRegionsAction(blockRegion);
        action.doAction(developerView);
        actionsQueue.addNewAction(action);
    }

    private void mergeCells(int row, int column, int rowspan, int colspan) {
        var developerView = developerView();
        int height = Tool.height(developerView.getRegion());
        int width = Tool.width(developerView.getRegion());
        // Span subtraction (not row + rowspan) keeps the bounds check safe from int overflow on huge spans.
        boolean withinBounds = row >= 0 && row < height && column >= 0 && column < width
                && rowspan >= 1 && colspan >= 1 && rowspan <= height - row && colspan <= width - column;
        if (!withinBounds || (rowspan < 2 && colspan < 2)) {
            throw new BadRequestException("table.action.merge.range.invalid.message",
                    new Object[]{row, column, rowspan, colspan});
        }
        requireNoConflictingMerge(developerView, row, column, rowspan, colspan);
        requireNoMergeDataLoss(developerView, row, column, rowspan, colspan);
        var region = new GridRegion(row, column, row + rowspan - 1, column + colspan - 1);
        applyMergeRegions(developerView, List.of(region));
        // Blank the now-covered cells so the merge keeps no hidden orphan value under the span, which would
        // otherwise resurface on unmerge and block later structural edits (delete row/column).
        clearCoveredCells(developerView, row, column, rowspan, colspan);
    }

    private void clearCoveredCells(IGridTable developerView, int row, int column, int rowspan, int colspan) {
        for (int r = 0; r < rowspan; r++) {
            for (int c = 0; c < colspan; c++) {
                if (r != 0 || c != 0) {
                    createOrUpdateCell(developerView, buildCellKey(column + c, row + r), null);
                }
            }
        }
    }

    /**
     * Rejects a merge that would discard data.
     * <p>
     * Merging keeps only the top-left cell and hides the rest. When the range holds more than one distinct non-empty
     * value, merging would silently drop the others, so the edit is refused. Empty cells and repeated equal values are
     * allowed because nothing is lost.
     */
    private static void requireNoMergeDataLoss(IGridTable developerView, int row, int column, int rowspan, int colspan) {
        var tableRegion = developerView.getRegion();
        var grid = developerView.getGrid();
        Set<String> distinctValues = new HashSet<>();
        for (int r = 0; r < rowspan; r++) {
            for (int c = 0; c < colspan; c++) {
                String value = grid.getCell(tableRegion.getLeft() + column + c, tableRegion.getTop() + row + r)
                        .getStringValue();
                if (value != null && !value.isBlank()) {
                    distinctValues.add(value);
                }
            }
        }
        if (distinctValues.size() > 1) {
            throw new BadRequestException("table.action.merge.data-loss.message", new Object[]{row, column});
        }
    }

    private void unmergeCells(int row, int column) {
        var developerView = developerView();
        requireCellInBounds(developerView, row, column);
        var tableRegion = developerView.getRegion();
        var merged = developerView.getGrid()
                .getRegionContaining(tableRegion.getLeft() + column, tableRegion.getTop() + row);
        if (merged == null) {
            throw new BadRequestException("table.action.unmerge.not-merged.message",
                    new Object[]{row, column});
        }
        // Remove exactly the found merge through the undoable action (consistent with the rest of the writer).
        var action = new RemoveMergedRegionsAction(merged);
        action.doAction(developerView);
        actionsQueue.addNewAction(action);
    }

    /**
     * Removes the merges anchored in a single row or column. The line-sized counterpart of
     * {@link #clearMergedRegions}: an update re-applies only the merges declared in its new cells, so this drops the
     * ones that were there before but are absent now (they would otherwise mask the new values).
     */
    private void clearLineMerges(IGridTable developerView, int index, boolean horizontal) {
        var tableRegion = developerView.getRegion();
        GridRegion lineRegion = horizontal
                ? new GridRegion(tableRegion.getTop() + index, tableRegion.getLeft(),
                        tableRegion.getTop() + index, tableRegion.getRight())
                : new GridRegion(tableRegion.getTop(), tableRegion.getLeft() + index,
                        tableRegion.getBottom(), tableRegion.getLeft() + index);
        var action = new RemoveMergedRegionsAction(lineRegion);
        action.doAction(developerView);
        actionsQueue.addNewAction(action);
    }

    private IGridTable developerView() {
        return table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);
    }

    private void insertBlankRows(IGridTable developerView, int beforeRow) {
        var action = new UndoableInsertRowsAction(1, beforeRow, 0, getMetaInfoWriter());
        action.doAction(developerView);
        actionsQueue.addNewAction(action);
    }

    private void insertBlankColumns(IGridTable developerView, int beforeColumn) {
        var action = new UndoableInsertColumnsAction(1, beforeColumn, 0, getMetaInfoWriter());
        action.doAction(developerView);
        actionsQueue.addNewAction(action);
    }

    private void writeRow(IGridTable developerView, int row, List<RawCellInput> cells, boolean skipCovered) {
        writeLine(developerView, cells, row, true, skipCovered);
    }

    private void writeColumn(IGridTable developerView, int column, List<RawCellInput> cells, boolean skipCovered) {
        writeLine(developerView, cells, column, false, skipCovered);
    }

    /**
     * Write a line of raw cells, applying merge regions for spanning cells. A horizontal line keeps {@code fixedIndex}
     * as its row and walks the columns; a vertical line keeps it as its column and walks the rows. Covered cells are
     * skipped because their value lives in the spanning origin cell.
     */
    private void writeLine(IGridTable developerView, List<RawCellInput> cells, int fixedIndex, boolean horizontal,
                           boolean skipCovered) {
        // Appending writes one index past the edge, so the table grows by one along the line's axis.
        int spanWidth = horizontal ? Tool.width(developerView.getRegion())
                : Math.max(Tool.width(developerView.getRegion()), fixedIndex + 1);
        int spanHeight = horizontal ? Math.max(Tool.height(developerView.getRegion()), fixedIndex + 1)
                : Tool.height(developerView.getRegion());
        List<IGridRegion> mergeRegions = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            int row = horizontal ? fixedIndex : i;
            int col = horizontal ? i : fixedIndex;
            writeCellInput(developerView, row, col, cells.get(i), skipCovered, spanWidth, spanHeight, mergeRegions);
        }
        applyMergeRegions(developerView, mergeRegions);
    }

    /**
     * Writes one input cell at an absolute table position and queues any inline merge. Blank and explicitly-covered
     * inputs are skipped. When {@code skipCovered} is set (an update), a position masked by an existing merge is
     * rejected: writing it would leave invisible "orphan" content that later corrupts structural edits, so callers
     * must mark such positions {@code "covered": true}. Insert/append target fresh cells, so they pass it unset.
     */
    private void writeCellInput(IGridTable developerView, int row, int col, RawCellInput cell, boolean skipCovered,
                                int width, int height, List<IGridRegion> mergeRegions) {
        if (cell == null || Boolean.TRUE.equals(cell.covered())) {
            return;
        }
        if (skipCovered && isCoveredByMerge(developerView, row, col)) {
            throw new BadRequestException("table.action.cell.covered.message", new Object[]{row, col});
        }
        requireSpanInBounds(cell, row, col, width, height);
        createOrUpdateCell(developerView, buildCellKey(col, row), cell.value());
        // Validate an inline span (colspan/rowspan) against existing merges, like the explicit merge action, so an
        // update cannot silently create a merge that straddles one already on the sheet.
        buildMergeRegionIfNeeded(cell.colspan(), cell.rowspan(), row, col).ifPresent(region -> {
            requireNoConflictingMerge(developerView, row, col,
                    cell.rowspan() == null ? 1 : cell.rowspan(),
                    cell.colspan() == null ? 1 : cell.colspan());
            mergeRegions.add(region);
        });
    }

    private static List<RawCellInput> requireCells(List<RawCellInput> cells) {
        if (cells == null || cells.isEmpty()) {
            throw new BadRequestException("table.action.cells.required.message");
        }
        return cells;
    }

    private static void requirePosition(int position, int minInclusive, int maxInclusive) {
        if (position < minInclusive || position > maxInclusive) {
            throw new BadRequestException("table.action.position.invalid.message",
                    new Object[]{position, minInclusive, maxInclusive});
        }
    }

    private static void requireDeletable(int size) {
        if (size <= 1) {
            throw new BadRequestException("table.action.delete.last.message");
        }
    }

    private static void requireCellInBounds(IGridTable developerView, int row, int column) {
        int height = Tool.height(developerView.getRegion());
        int width = Tool.width(developerView.getRegion());
        if (row < 0 || row >= height || column < 0 || column >= width) {
            throw new BadRequestException("table.action.cell.out-of-bounds.message",
                    new Object[]{row, column, height - 1, width - 1});
        }
    }

    /**
     * Tells whether the cell at the given table position is masked by an existing merged region — i.e. it sits inside
     * a merge but is not its top-left origin. Such cells have no value of their own in the raw view.
     */
    private static boolean isCoveredByMerge(IGridTable developerView, int row, int column) {
        var region = developerView.getRegion();
        int absColumn = region.getLeft() + column;
        int absRow = region.getTop() + row;
        var merged = developerView.getGrid().getRegionContaining(absColumn, absRow);
        return merged != null && (merged.getLeft() != absColumn || merged.getTop() != absRow);
    }

    private static void requireSpanInBounds(RawCellInput cell, int row, int col, int width, int height) {
        int colspan = cell.colspan() != null ? cell.colspan() : 1;
        int rowspan = cell.rowspan() != null ? cell.rowspan() : 1;
        if (colspan > width - col || rowspan > height - row) {
            throw new BadRequestException("table.action.span.out-of-bounds.message",
                    new Object[]{row, col, rowspan, colspan});
        }
    }

    /**
     * Rejects a merge that would only straddle an existing merged region. A region whose top-left corner falls inside
     * the new range is replaced by the merge, but one that overlaps without its corner inside would leave two
     * overlapping merges in the sheet.
     */
    private static void requireNoConflictingMerge(IGridTable developerView, int row, int column,
                                                  int rowspan, int colspan) {
        var tableRegion = developerView.getRegion();
        int left = tableRegion.getLeft() + column;
        int top = tableRegion.getTop() + row;
        int right = left + colspan - 1;
        int bottom = top + rowspan - 1;
        var grid = developerView.getGrid();
        for (int i = 0, n = grid.getNumberOfMergedRegions(); i < n; i++) {
            requireNotStraddling(grid.getMergedRegion(i), left, top, right, bottom, row, column);
        }
    }

    private static void requireNotStraddling(IGridRegion existing, int left, int top, int right, int bottom,
                                             int row, int column) {
        boolean intersects = left <= existing.getRight() && existing.getLeft() <= right
                && top <= existing.getBottom() && existing.getTop() <= bottom;
        boolean cornerInside = existing.getLeft() >= left && existing.getLeft() <= right
                && existing.getTop() >= top && existing.getTop() <= bottom;
        if (intersects && !cornerInside) {
            throw new BadRequestException("table.action.merge.overlap.message", new Object[]{row, column});
        }
    }

    private static void requireRowWidth(List<RawCellInput> cells, int width) {
        requireLineLength(cells, width, "table.action.row.width.message");
    }

    private static void requireColumnHeight(List<RawCellInput> cells, int height) {
        requireLineLength(cells, height, "table.action.column.height.message");
    }

    private static void requireLineLength(List<RawCellInput> cells, int limit, String messageKey) {
        // A row must carry one cell per column and a column one cell per row. Too few cells would silently leave
        // trailing cells empty, too many would grow the table, so the count must match the table dimension exactly.
        if (cells.size() != limit) {
            throw new BadRequestException(messageKey, new Object[]{cells.size(), limit});
        }
    }

    private static int requireRectangularRange(List<List<RawCellInput>> cells) {
        int rangeWidth = cells.get(0).size();
        boolean uneven = cells.stream().anyMatch(rowCells -> rowCells.size() != rangeWidth);
        if (uneven || rangeWidth == 0) {
            throw new BadRequestException("table.action.range.not-rectangular.message");
        }
        return rangeWidth;
    }

    private static void requireRangeMultiCell(int rangeHeight, int rangeWidth) {
        // A single cell is the cell update's job; a range must cover more than one cell.
        if (rangeHeight == 1 && rangeWidth == 1) {
            throw new BadRequestException("table.action.range.single-cell.message");
        }
    }

    private static void requireRangeInBounds(int row, int column, int rangeHeight, int rangeWidth,
                                             int height, int width) {
        // Span subtraction (not row + rangeHeight) keeps the bounds check safe from int overflow on huge ranges.
        if (row < 0 || column < 0 || rangeHeight > height - row || rangeWidth > width - column) {
            throw new BadRequestException("table.action.range.out-of-bounds.message",
                    new Object[]{row, column, rangeHeight, rangeWidth, height - 1, width - 1});
        }
    }

    private static void requireMultiLine(List<List<RawCellInput>> lines) {
        // A single row or column is the row/column action's job; a block must hold more than one line.
        if (lines == null || lines.size() < 2) {
            throw new BadRequestException("table.action.range.single-line.message");
        }
    }

    private static void requireBlockCount(int count) {
        // A single row or column is the row/column action's job; a block deletes more than one line.
        if (count < 2) {
            throw new BadRequestException("table.action.range.single-line.message");
        }
    }

}
