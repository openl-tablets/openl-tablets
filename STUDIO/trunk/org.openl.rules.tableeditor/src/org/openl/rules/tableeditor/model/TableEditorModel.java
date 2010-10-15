/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.tableeditor.model;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.AGridTableDecorator;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.IUndoableAction;
import org.openl.rules.table.actions.IUndoableGridAction;
import org.openl.rules.table.actions.UndoableActions;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableMoveTableAction;
import org.openl.rules.table.actions.GridRegionAction.ActionType;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.ui.filters.SimpleFormatFilter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.tableeditor.renderkit.TableEditor;
import org.openl.util.formatters.IFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author snshor
 */
public class TableEditorModel {

    public static final boolean COLUMNS = true;
    public static final boolean ROWS = false;
    public static final boolean INSERT = true;
    public static final boolean REMOVE = false;

    /** Number of columns in Properties section */
    public static final int NUMBER_PROPERTIES_COLUMNS = 3;

    private IOpenLTable table;
    private IGridTable gridTable;
    private IGridRegion fullTableRegion;
    private IGridRegion displayedTableRegion;
    private String view;
    private boolean showFormulas = false;
    private boolean collapseProps = false;
    private String beforeSaveAction;
    private String afterSaveAction;
    private String saveFailureAction;

    private IGridTable[] othertables;

    private UndoableActions actions = new UndoableActions();

    private FilteredGrid filteredGrid;

    private TableEditor tableEditor;

    public TableEditorModel(TableEditor editor) {
        this(editor.getTable(), editor.getView(), editor.isShowFormulas());
        setTableEditor(editor);
    }

    public TableEditorModel(IOpenLTable table, String view, boolean showFormulas) {
        this.table = table;
        this.view = view;
        this.gridTable = table.getGridTable(view);
        this.showFormulas = showFormulas;
        fullTableRegion = new GridRegion(getOriginalTable(this.gridTable).getRegion());
        displayedTableRegion = new GridRegion(gridTable.getRegion());
        othertables = gridTable.getGrid().getTables();

        removeThisTable(othertables);
        makeFilteredGrid(gridTable);
    }

    public boolean isBusinessView() {
        return view != null && view.equalsIgnoreCase(IXlsTableNames.VIEW_BUSINESS);
    }
    
    public boolean canInsertCols(int nCols) {
        GridRegion testRegion = new GridRegion(fullTableRegion.getTop() - 1, fullTableRegion.getRight() + 1,
                fullTableRegion.getBottom() + 1, fullTableRegion.getRight() + 1 + nCols);
        for (int i = 0; i < othertables.length; i++) {
            if (IGridRegion.Tool.intersects(testRegion, othertables[i].getRegion())) {
                return false;
            }
        }
        return true;
    }

    public boolean canInsertRows(int nRows) {
        GridRegion testRegion = new GridRegion(fullTableRegion.getBottom() + 1, fullTableRegion.getLeft() - 1,
                fullTableRegion.getBottom() + 1 + nRows, fullTableRegion.getRight() + 1);
        for (int i = 0; i < othertables.length; i++) {
            if (IGridRegion.Tool.intersects(testRegion, othertables[i].getRegion())) {
                return false;
            }
        }
        return true;
    }

    public synchronized void cancel() {
        while (actions.hasUndo()) {
            undo();
        }
    }

    public ICell getCell(int row, int column) {
        return gridTable.getGrid().getCell(tX(column), tY(row));
    }

    private IFormatter getFormatter(int col, int row) {
        IFormatter formatter = null;
        FormattedCell fc = filteredGrid.getFormattedCell(
                fullTableRegion.getLeft() + col, fullTableRegion.getTop() + row);

        if (fc != null) {
            IGridFilter filter = fc.getFilter();
            if (filter != null) {
                formatter = filter.getFormatter();
            }
        }

        return formatter;
    }

    public IOpenLTable getTable() {
        return table;
    }

    public synchronized IGridTable getUpdatedTable() {
        return new GridTable(displayedTableRegion, gridTable.getGrid());

    }

    public synchronized IGridTable getUpdatedFullTable() {
        return new GridTable(fullTableRegion, gridTable.getGrid());

    }

    public synchronized boolean hasRedo() {
        return actions.hasRedo();
    }

    public synchronized boolean hasUndo() {
        return actions.hasUndo();
    }

    /**
     * Extracts original table.
     * 
     * @param table Table which we have.
     * @return Original table that includes our table.
     */
    public static IGridTable getOriginalTable(IGridTable table) {
        while (table instanceof AGridTableDecorator) {
            table = ((AGridTableDecorator) table).getOriginalGridTable();
        }
        return table;
    }

    public synchronized void insertColumns(int nCols, int beforeCol, int row) throws Exception {
        IUndoableGridAction moveTableAction = null;
        if (!canInsertCols(1)) {
            moveTableAction = moveTable(getUpdatedFullTable());
        }
        int cellWidth = getCell(row, beforeCol).getWidth();
        if (cellWidth > 1) { // merged cell
            nCols += cellWidth - 1;
        }
        IUndoableGridAction ua = IWritableGrid.Tool.insertColumns(nCols, beforeCol, fullTableRegion, wgrid());
        GridRegionAction allTable = new GridRegionAction(fullTableRegion, COLUMNS, INSERT, ActionType.EXPAND, nCols);
        GridRegionAction displayedTable = new GridRegionAction(displayedTableRegion, COLUMNS, INSERT,
                ActionType.EXPAND, nCols);
        UndoableCompositeAction action = new UndoableCompositeAction(ua, allTable, displayedTable);
        action.doAction(wgrid());
        if (moveTableAction != null) {
            action = new UndoableCompositeAction(moveTableAction, action);
        }
        actions.addNewAction(action);
    }

    public synchronized void insertRows(int nRows, int beforeRow, int col) throws Exception {
        IUndoableGridAction moveTableAction = null;
        if (!canInsertRows(1)) {
            moveTableAction = moveTable(getUpdatedFullTable());
        }
        int cellHeight = getCell(beforeRow, col).getHeight();
        if (cellHeight > 1) { // merged cell
            nRows += cellHeight - 1;
        }
        IUndoableGridAction ua = IWritableGrid.Tool.insertRows(nRows, beforeRow, fullTableRegion, wgrid());

        GridRegionAction allTable = new GridRegionAction(fullTableRegion, ROWS, INSERT, ActionType.EXPAND, nRows);
        GridRegionAction displayedTable = new GridRegionAction(displayedTableRegion, ROWS, INSERT, ActionType.EXPAND,
                nRows);
        UndoableCompositeAction action = new UndoableCompositeAction(ua, allTable, displayedTable);
        action.doAction(wgrid());
        if (moveTableAction != null) {
            action = new UndoableCompositeAction(moveTableAction, action);
        }
        actions.addNewAction(action);
    }

    private void makeFilteredGrid(IGridTable gt) {
        IGrid g = gt.getGrid();
        if (g instanceof FilteredGrid) {
            filteredGrid = (FilteredGrid) g;
            return;
        }

        filteredGrid = new FilteredGrid(gt.getGrid(), new IGridFilter[] { new SimpleFormatFilter() });
    }

    public synchronized void redo() {
        IUndoableAction ua = actions.getRedoAction();
        ((IUndoableGridAction) ua).doAction(wgrid());
    }

    public synchronized void removeColumns(int nCols, int startCol, int row) {
        if (startCol < 0 || startCol >= IGridRegion.Tool.width(fullTableRegion)) {
            return;
        }
        int cellWidth = getCell(row, startCol).getWidth();
        if (cellWidth > 1) { // merged cell
            nCols += cellWidth - 1;
        }
        IUndoableGridAction ua = IWritableGrid.Tool.removeColumns(nCols, startCol, fullTableRegion, wgrid());
        GridRegionAction allTable = new GridRegionAction(fullTableRegion, COLUMNS, REMOVE, ActionType.EXPAND, nCols);
        GridRegionAction displayedTable = new GridRegionAction(displayedTableRegion, COLUMNS, REMOVE,
                ActionType.EXPAND, nCols);
        UndoableCompositeAction action = new UndoableCompositeAction(ua, allTable, displayedTable);
        action.doAction(wgrid());
        actions.addNewAction(action);
    }

    public synchronized void removeRows(int nRows, int startRow, int col) {
        if (startRow < 0 || startRow >= IGridRegion.Tool.height(fullTableRegion)) {
            return;
        }
        int cellHeight = getCell(startRow, col).getHeight();
        if (cellHeight > 1) { // merged cell
            nRows += cellHeight - 1;
        }
        IUndoableGridAction ua = IWritableGrid.Tool.removeRows(nRows, startRow, fullTableRegion, wgrid());
        GridRegionAction allTable = new GridRegionAction(fullTableRegion, ROWS, REMOVE, ActionType.EXPAND, nRows);
        GridRegionAction displayedTable = new GridRegionAction(displayedTableRegion, ROWS, REMOVE, ActionType.EXPAND,
                nRows);
        UndoableCompositeAction action = new UndoableCompositeAction(ua, allTable, displayedTable);
        action.doAction(wgrid());
        actions.addNewAction(action);
    }

    /**
     * @param otherTables
     * 
     */
    private synchronized void removeThisTable(IGridTable[] otherTables) {
        Vector<IGridTable> v = new Vector<IGridTable>();
        for (int i = 0; i < otherTables.length; i++) {
            if (!IGridRegion.Tool.intersects(otherTables[i].getRegion(), fullTableRegion)) {
                v.add(otherTables[i]);
            }
        }

        othertables = v.toArray(new IGridTable[v.size()]);
    }
    
    /**     
     * @return New table URI on the sheet where it was saved. It is needed for tables that were moved
     * to new place during adding new rows and columns on editing. We need to know new destination of the table.
     * @throws IOException
     */
    public synchronized String save() throws IOException {
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();        
        String newTableUri = xlsgrid.getRangeUri(fullTableRegion);        
        xlsgrid.getSheetSource().getWorkbookSource().save();
        actions = new UndoableActions();
        return newTableUri;
    }
    
    /**
     * @return Sheet source of editable table
     */
    public XlsSheetSourceCodeModule getSheetSource(){
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        return xlsgrid.getSheetSource();
    }

    public synchronized void saveAs(String fname) throws IOException {
        getSheetSource().getWorkbookSource().saveAs(fname);
    }

    public synchronized void setCellValue(int row, int col, String value) {
        IUndoableGridAction action = IWritableGrid.Tool.setStringValue(
                col, row, fullTableRegion, value, getFormatter(col, row));
        action.doAction(wgrid());
        actions.addNewAction(action);
    }

    public synchronized void setProperty(String name, String value) throws Exception {
        List<IUndoableGridAction> createdActions = new ArrayList<IUndoableGridAction>();
        int nRowsToInsert = 0;
        int nColsToInsert = 0;

        CellKey propertyCoordinates = IWritableGrid.Tool.getPropertyCoordinates(fullTableRegion, wgrid(), name);

        boolean propExists = propertyCoordinates != null;
        boolean propIsBlank = StringUtils.isBlank(value);

        if (!propIsBlank && !propExists) {
            IGridTable table = getUpdatedFullTable();
            int tableWidth = table.getWidth();
            if (tableWidth < NUMBER_PROPERTIES_COLUMNS) {
                nColsToInsert = NUMBER_PROPERTIES_COLUMNS - tableWidth;
            }
            nRowsToInsert = 1;
            if ((nRowsToInsert > 0 && !canInsertRows(nRowsToInsert))
                    || !canInsertCols(nColsToInsert)) {
                createdActions.add(moveTable(table));
            }
            GridRegionAction allTable = new GridRegionAction(fullTableRegion, ROWS, INSERT, ActionType.EXPAND,
                    nRowsToInsert);
            allTable.doAction(wgrid());
            createdActions.add(allTable);
            GridRegionAction displayedTable = new GridRegionAction(displayedTableRegion, ROWS, INSERT,
                    isBusinessView() ? ActionType.MOVE : ActionType.EXPAND, nRowsToInsert);
            displayedTable.doAction(wgrid());
            createdActions.add(displayedTable);
        } else if (propIsBlank && propExists) {
            removeRows(1, propertyCoordinates.getRow(), propertyCoordinates.getColumn());
            return;
        }

        IUndoableGridAction action = IWritableGrid.Tool
            .insertProp(fullTableRegion, displayedTableRegion, wgrid(), name, value);
        if (action != null) {
            action.doAction(wgrid());
            createdActions.add(action);
        }
        if (!createdActions.isEmpty()) {
            actions.addNewAction(new UndoableCompositeAction(createdActions));
        }
    }

    /**
     * After coping or moving the table we need to set new region destination of
     * it
     * 
     * @param newRegion New region of the table
     */
    public IUndoableGridAction setRegion(IGridRegion newRegion) {
        List<IUndoableGridAction> actions = new ArrayList<IUndoableGridAction>();
        int topOffset = newRegion.getTop() - fullTableRegion.getTop();
        int leftOffset = newRegion.getLeft() - fullTableRegion.getLeft();
        if (topOffset != 0) {
            actions.add(new GridRegionAction(fullTableRegion, ROWS, INSERT, ActionType.MOVE, topOffset));
            actions.add(new GridRegionAction(displayedTableRegion, ROWS, INSERT, ActionType.MOVE, topOffset));
        }
        if (leftOffset != 0) {
            actions.add(new GridRegionAction(fullTableRegion, COLUMNS, INSERT, ActionType.MOVE, leftOffset));
            actions.add(new GridRegionAction(displayedTableRegion, COLUMNS, INSERT, ActionType.MOVE, leftOffset));
        }
        return new UndoableCompositeAction(actions);
    }

    /**
     * Creates actions that moves the table and executes these actions.
     * 
     * @param table Table to move.
     * @return Actions that moved the table.
     * @throws Exception
     */
    public synchronized IUndoableGridAction moveTable(IGridTable table) throws Exception {
        UndoableMoveTableAction moveTableAction = new UndoableMoveTableAction(table);
        moveTableAction.doAction(wgrid());
        IUndoableGridAction changeRegions = setRegion(moveTableAction.getNewRegion());
        changeRegions.doAction(wgrid());
        return new UndoableCompositeAction(moveTableAction, changeRegions);
    }

    public synchronized void setStyle(int row, int col, ICellStyle style) {
        IUndoableGridAction ua = IWritableGrid.Tool.setStyle(col, row, fullTableRegion, style);
        ua.doAction(wgrid());
        actions.addNewAction(ua);
    }

    public int tX(int col) {
        return fullTableRegion.getLeft() + col;
    }

    public int tY(int row) {
        return fullTableRegion.getTop() + row;
    }

    /**
     * @return Count of rows that is not showed.
     */
    public int getNumberOfNonShownRows() {
        return Tool.height(fullTableRegion) - Tool.height(displayedTableRegion);
    }

    /**
     * @return Count of columns that is not showed.
     */
    public int getNumberOfNonShownCols() {
        return Tool.width(fullTableRegion) - Tool.width(displayedTableRegion);
    }

    public synchronized void undo() {
        IUndoableAction ua = actions.getUndoAction();
        ((IUndoableGridAction) ua).undoAction(wgrid());
    }

    private IWritableGrid wgrid() {
        return (IWritableGrid) gridTable.getGrid();
    }

    public boolean isShowFormulas() {
        return showFormulas;
    }

    public void setShowFormulas(boolean showFormulas) {
        this.showFormulas = showFormulas;
    }

    public boolean isCollapseProps() {
        return collapseProps;
    }

    public void setCollapseProps(boolean collapseProps) {
        this.collapseProps = collapseProps;
    }

    public void setBeforeSaveAction(String beforeSaveAction) {
        this.beforeSaveAction = beforeSaveAction;
    }

    public String getBeforeSaveAction() {
        return beforeSaveAction;
    }

    public void setAfterSaveAction(String afterSaveAction) {
        this.afterSaveAction = afterSaveAction;
    }

    public String getAfterSaveAction() {
        return afterSaveAction;
    }
    
    public String getSaveFailureAction() {
        return saveFailureAction;
    }

    public void setSaveFailureAction(String saveFailureAction) {
        this.saveFailureAction = saveFailureAction;
    }

    public void setTableEditor(TableEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public TableEditor getTableEditor() {
        return tableEditor;
    }
}