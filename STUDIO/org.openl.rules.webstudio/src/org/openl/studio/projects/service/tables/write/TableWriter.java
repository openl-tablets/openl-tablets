package org.openl.studio.projects.service.tables.write;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriterImpl;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.CellKey.CellKeyFactory;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.actions.MergeCellsAction;
import org.openl.rules.table.actions.UndoableActions;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableEditTableAction;
import org.openl.rules.table.actions.UndoableInsertColumnsAction;
import org.openl.rules.table.actions.UndoableInsertRowsAction;
import org.openl.rules.table.actions.UndoableRemoveColumnsAction;
import org.openl.rules.table.actions.UndoableRemoveRowsAction;
import org.openl.rules.table.actions.UndoableSetValueAction;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.common.utils.XSSFOptimizer;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Base class for table writers.
 *
 * @author Vladyslav Pikus
 */
public abstract class TableWriter<T extends TableView> {

    protected static final int NUMBER_PROPERTIES_COLUMNS = 3;

    protected final UndoableActions actionsQueue;
    protected final IOpenLTable table;
    protected final IGridTable originalTable;
    private MetaInfoWriter metaInfoWriter;

    public TableWriter(IOpenLTable table) {
        this.table = table;
        this.actionsQueue = new UndoableActions();
        this.originalTable = GridTableUtils.getOriginalTable(table.getGridTable());
    }

    protected TableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        this.originalTable = gridTable;
        this.metaInfoWriter = metaInfoWriter;
        this.table = null;
        this.actionsQueue = new UndoableActions();
    }

    public void write(T tableView) {
        try {
            getGridTable().edit();
            updateBusinessBody(tableView);
            updateTableProperties(tableView.properties);
            updateHeader(tableView);
            save();
        } finally {
            getGridTable().stopEditing();
        }
    }

    protected abstract void updateHeader(T tableView);

    protected abstract void updateBusinessBody(T tableView);

    protected void updateTableProperties(Map<String, Object> properties) {
        var originalRegion = originalTable.getRegion();
        var originalGrid = originalTable.getGrid();
        for (var entry : properties.entrySet()) {
            List<IUndoableGridTableAction> actions = new ArrayList<>();
            var propName = entry.getKey();
            boolean newProperty = GridTool.getPropertyRowIndex(originalRegion, originalGrid, propName) == -1;
            if (newProperty) {
                int tableWidth = Tool.width(originalRegion);
                int nColsToInsert = 0;
                if (tableWidth < NUMBER_PROPERTIES_COLUMNS) {
                    nColsToInsert = NUMBER_PROPERTIES_COLUMNS - tableWidth;
                }
                if (!UndoableInsertRowsAction.canInsertRows(originalTable, 1) || !UndoableInsertColumnsAction
                        .canInsertColumns(originalTable, nColsToInsert)) {
                    actions.add(UndoableEditTableAction.moveTable(originalTable, getMetaInfoWriter()));
                }
                GridRegionAction allTable = new GridRegionAction(originalRegion,
                        UndoableEditTableAction.ROWS,
                        UndoableEditTableAction.INSERT,
                        GridRegionAction.ActionType.EXPAND,
                        1);
                allTable.doAction(originalTable);
                actions.add(allTable);
            }
            var propValue = entry.getValue();
            var action = GridTool.insertProp(originalRegion, originalGrid, propName, propValue, getMetaInfoWriter());
            if (action != null) {
                action.doAction(originalTable);
                actions.add(action);
            }
            if (!actions.isEmpty()) {
                actionsQueue.addNewAction(new UndoableCompositeAction(actions));
            }
        }
    }

    /**
     * Creates or updates cell in the table.
     *
     * @param gridTable table to update
     * @param cellKey   relative cell coordinates
     * @param value     new cell value
     */
    protected void createOrUpdateCell(IGridTable gridTable, CellKey cellKey, Object value) {
        List<IUndoableGridTableAction> actions = new ArrayList<>();
        var region = gridTable.getRegion();
        // calculate absolute coordinates
        int grow = cellKey.getRow() + region.getTop();
        // check if cell is out of table
        boolean inserted = false;
        if (region.getBottom() <= grow) {
            int nRows = grow - region.getBottom();
            int beforeRow = region.getTop() - originalTable.getRegion().getTop() + Tool.height(region);
            actions.add(insertRows(gridTable, nRows, beforeRow));
            inserted = true;
        }
        int gcol = cellKey.getColumn() + region.getLeft();
        if (region.getRight() <= gcol) {
            int nCols = gcol - region.getRight();
            int beforeCol = region.getLeft() - originalTable.getRegion().getLeft() + Tool.width(region);
            actions.add(insertColumns(gridTable, nCols, beforeCol));
            inserted = true;
        }
        if (inserted) {
            // recalculate absolute coordinates because it may be changed after inserting columns
            grow = cellKey.getRow() + region.getTop();
            gcol = cellKey.getColumn() + region.getLeft();
        }

        actions.add(updateCellValue(gridTable, gcol, grow, value));
        actionsQueue.addNewAction(new UndoableCompositeAction(actions));
    }

    private IUndoableGridTableAction insertColumns(IGridTable gridTable, int nCols, int beforeCol) {
        var action = new UndoableInsertColumnsAction(nCols, beforeCol, 0, getMetaInfoWriter());
        action.doAction(gridTable);
        return action;
    }

    private IUndoableGridTableAction insertRows(IGridTable gridTable, int nRows, int beforeRow) {
        var action = new UndoableInsertRowsAction(nRows, beforeRow, 0, getMetaInfoWriter());
        action.doAction(gridTable);
        return action;
    }

    @SuppressWarnings("rawtypes")
    private IUndoableGridTableAction updateCellValue(IGridTable gridTable, int gcol, int grow, Object value) {
        if (value instanceof Collection collection) {
            // OpenL table cell can store only array, not collection
            value = collection.toArray();
        }
        var action = new UndoableSetValueAction(gcol, grow, value, getMetaInfoWriter());
        action.doAction(gridTable);
        return action;
    }

    protected void removeRows(IGridTable gridTable, int nRows, int startRow) {
        int startRow0 = gridTable.getRegion().getTop() - originalTable.getRegion().getTop() + startRow;
        var action = new UndoableRemoveRowsAction(nRows, startRow0, getMetaInfoWriter());
        action.doAction(gridTable);
        actionsQueue.addNewAction(action);
    }

    protected void removeColumns(IGridTable gridTable, int nCols, int startCol) {
        int startCol0 = gridTable.getRegion().getLeft() - originalTable.getRegion().getLeft() + startCol;
        var action = new UndoableRemoveColumnsAction(nCols, startCol0, getMetaInfoWriter());
        action.doAction(gridTable);
        actionsQueue.addNewAction(action);
    }

    private MetaInfoWriter getMetaInfoWriter() {
        if (metaInfoWriter == null && isUpdateMode()) {
            metaInfoWriter = new MetaInfoWriterImpl(table.getMetaInfoReader(), table.getGridTable());
        }
        return metaInfoWriter;
    }

    protected void save() {
        try {
            var xlsgrid = (XlsSheetGridModel) getGridTable().getGrid();
            var workbook = xlsgrid.getSheetSource().getWorkbookSource();
            if (workbook.getWorkbook() instanceof XSSFWorkbook xssfWorkbook) {
                XSSFOptimizer.removeUnusedStyles(xssfWorkbook);
            }
            workbook.save();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    protected CellKey buildCellKey(int col, int row) {
        return CellKeyFactory.getCellKey(col, row);
    }

    protected String getBusinessTableType(T tableView) {
        return isUpdateMode()
                ? OpenLTableUtils.getTableTypeItems().get(table.getType())
                : tableView.tableType;
    }

    /**
     * Apply merge regions to the grid.
     * <p>
     * Creates MergeCellsAction for each merge region and executes them.
     * This should be called after all cell values are written to ensure
     * grid dimensions are correct.
     *
     * @param gridTable    The grid table to apply merges to
     * @param mergeRegions The list of regions to merge
     */
    protected void applyMergeRegions(IGridTable gridTable, List<IGridRegion> mergeRegions) {
        var tableRegion = gridTable.getRegion();
        List<IUndoableGridTableAction> mergeActions = mergeRegions.stream()
                .map(mr -> toAbsolute(mr, tableRegion))
                .map(MergeCellsAction::new)
                .collect(Collectors.toList());

        if (!mergeActions.isEmpty()) {
            var compositeAction = new UndoableCompositeAction(mergeActions);
            compositeAction.doAction(gridTable);
            actionsQueue.addNewAction(compositeAction);
        }
    }

    private IGridRegion toAbsolute(IGridRegion mr, IGridRegion tableRegion) {
        int top = mr.getTop() + tableRegion.getTop();
        int left = mr.getLeft() + tableRegion.getLeft();
        int bottom = mr.getBottom() + tableRegion.getTop();
        int right = mr.getRight() + tableRegion.getLeft();
        return new GridRegion(top, left, bottom, right);
    }

    protected boolean isUpdateMode() {
        return table != null;
    }

    protected IGridTable getGridTable() {
        if (isUpdateMode()) {
            return table.getGridTable();
        } else {
            return originalTable;
        }
    }

    protected IGridTable getGridTable(String view) {
        if (isUpdateMode()) {
            return table.getGridTable(view);
        } else {
            if (IXlsTableNames.VIEW_BUSINESS.equals(view)) {
                return originalTable.getSubtable(0,
                        1,
                        originalTable.getWidth(),
                        originalTable.getHeight() - 1);
            } else {
                return originalTable;
            }
        }
    }

}
