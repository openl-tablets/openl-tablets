package org.openl.rules.rest.service.tables.write;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriterImpl;
import org.openl.rules.rest.model.tables.TableView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.CellKey.CellKeyFactory;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.actions.UndoableActions;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableEditTableAction;
import org.openl.rules.table.actions.UndoableInsertColumnsAction;
import org.openl.rules.table.actions.UndoableInsertRowsAction;
import org.openl.rules.table.actions.UndoableRemoveColumnsAction;
import org.openl.rules.table.actions.UndoableRemoveRowsAction;
import org.openl.rules.table.actions.UndoableSetValueAction;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Base class for table writers.
 *
 * @author Vladyslav Pikus
 */
public abstract class TableWriter<T extends TableView> {

    private static final int NUMBER_PROPERTIES_COLUMNS = 3;

    protected final UndoableActions actionsQueue;
    protected final IOpenLTable table;
    protected final IGridTable originalTable;
    private MetaInfoWriter metaInfoWriter;

    public TableWriter(IOpenLTable table) {
        this.table = table;
        this.actionsQueue = new UndoableActions();
        this.originalTable = GridTableUtils.getOriginalTable(table.getGridTable());
    }

    public void write(T tableView) {
        try {
            table.getGridTable().edit();
            updateBusinessBody(tableView);
            updateTableProperties(tableView.properties);
            updateHeader(tableView);
            save();
        } finally {
            table.getGridTable().stopEditing();
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

    private IUndoableGridTableAction updateCellValue(IGridTable gridTable, int gcol, int grow, Object value) {
        var action = new UndoableSetValueAction(gcol, grow, value, getMetaInfoWriter());
        action.doAction(gridTable);
        return action;
    }

    protected void removeRows(IGridTable gridTable, int nRows, int startRow) {
        int startRow0 = gridTable.getRegion().getTop() - originalTable.getRegion().getTop() + startRow;
        var action = new UndoableRemoveRowsAction(nRows, startRow0, 0, getMetaInfoWriter());
        action.doAction(gridTable);
        actionsQueue.addNewAction(action);
    }

    protected void removeColumns(IGridTable gridTable, int nCols, int startCol) {
        int startCol0 = gridTable.getRegion().getLeft() - originalTable.getRegion().getLeft() + startCol;
        var action = new UndoableRemoveColumnsAction(nCols, startCol0, 0, getMetaInfoWriter());
        action.doAction(gridTable);
        actionsQueue.addNewAction(action);
    }

    private MetaInfoWriter getMetaInfoWriter() {
        if (metaInfoWriter == null) {
            metaInfoWriter = new MetaInfoWriterImpl(table.getMetaInfoReader(), table.getGridTable());
        }
        return metaInfoWriter;
    }

    protected void save() {
        try {
            var xlsgrid = (XlsSheetGridModel) table.getGridTable().getGrid();
            xlsgrid.getSheetSource().getWorkbookSource().save();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    protected CellKey buildCellKey(int col, int row) {
        return CellKeyFactory.getCellKey(col, row);
    }

    protected String getBusinessTableType() {
        return OpenLTableUtils.getTableTypeItems().get(table.getType());
    }

}
