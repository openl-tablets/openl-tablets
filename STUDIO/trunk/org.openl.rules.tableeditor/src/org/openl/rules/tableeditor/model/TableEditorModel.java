/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.tableeditor.model;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.AGridTableDelegator;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
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
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.XlsUndoGrid;
import org.openl.rules.tableeditor.renderkit.TableEditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author snshor
 * 
 */
public class TableEditorModel {

    /**
     * Table Headers – syntax depends on first keyword
     */
    public static final String TH_CELL_TYPE = "TH_CELL_TYPE";

    /**
     * Condition/Action header
     */
    public static final String CA_HEADER_CELL_TYPE = "CA_HEADER_CELL_TYPE ";

    /**
     * Condition/Action Formula (advanced feature would be the code completion)
     */
    public static final String CA_FORMULA_CELL_TYPE = "CA_FORMULA_CELL_TYPE";

    /**
     * Condition/Action Parameter Definition (type name)
     */
    public static final String CA_PARAMETER_DEFINITION_CELL_TYPE = "CA_PARAMETER_DEFINITION_CELL_TYPE";

    /**
     * Condition/Action Display Column Header
     */
    public static final String CA_DISPLAY_COLUMN_HEADER_CELL_TYPE = "CA_DISPLAY_COLUMN_HEADER_CELL_TYPE";
    /**
     * Condition/Action value cell – the type is defined by Parameter Definition –
     * constrained data entry based on the Domain of the Parameter Type (also
     * known as Subtype in Exigen Rules; the RDF/OWL uses the word Range to
     * define what we used to call Domain, and the word Domain for what we in
     * Java call the Declaring Class or Declaring Type; we need to decide which
     * terminology we are going to use – I start to lean toward RDF/OWL even
     * though it will conflict with our Range classes – will be IntRange,
     * DoubleRange etc. – but the superclass should be renamed to
     * ArithmeticRange); the cell editors should include comboboxes for
     * enumerations, range validators for numbers, other types of specialized
     * editors – calendars, ranges; it also should allow to input formulas –
     * similar to Condition/Action formula if the first character was ‘=’.
     */
    public static final String CA_ENUMERATION_CELL_TYPE = "CA_ENUMERATION_CELL_TYPE";
    public static final String CA_NUMBER_CELL_TYPE = "CA_NUMBER_CELL_TYPE";

    public static final String CA_DATETIME_CELL_TYPE = "CA_DATETIME_CELL_TYPE";

    /**
     * Date Table Column Headers – constrained to the set of the allowed names +
     * continuations (like in address.zip)
     */
    public static final String DT_COLUMN_HEADER_CELL_TYPE = "DT_COLUMN_HEADER_CELL_TYPE";

    /**
     * Data Table Foreign Keys – constrained to the list of tables with the
     * specific type
     */
    public static final String DT_FOREIGN_KEY_CELL_TYPE = "DT_FOREIGN_KEY_CELL_TYPE";

    /**
     * Data Display Column Header
     */
    public static final String DD_COLUMN_HEADER_CELL_TYPE = "DD_COLUMN_HEADER_CELL_TYPE";
    /**
     * Data Cell – the same as for DT cell, except for formulas (for now)
     */
    public static final String DD_FORMULA_CELL_TYPE = "DD_FORMULA_CELL_TYPE";
    public static final String DD_ENUMERATION_CELL_TYPE = "DD_ENUMERATION_CELL_TYPE";
    public static final String DD_NUMBER_CELL_TYPE = "DD_NUMBER_CELL_TYPE";

    public static final String DD_DATETIME_CELL_TYPE = "DD_DATETIME_CELL_TYPE";

    /**
     * Specialty Cells – like Environment include and import cells
     */
    public static final String SPECIAL_CELL_TYPE = "SPECIAL_CELL_TYPE";

    static final boolean COLUMNS = true, ROWS = false, INSERT = true, REMOVE = false;

    private ITable table;
    private IGridTable gridTable;
    private IGridRegion fullTableRegion;
    private IGridRegion displayedTableRegion;
    private String view;
    private boolean showFormulas = false;
    private boolean collapseProps = false;
    private String beforeSaveAction;
    private String afterSaveAction;

    private GridTable[] othertables;

    private UndoableActions actions = new UndoableActions();

    private XlsUndoGrid undoGrid;

    FilteredGrid filteredGrid;

    private TableEditor tableEditor;

    public TableEditorModel(TableEditor editor) {
        this(editor.getTable(), editor.getView(), editor.isShowFormulas());
        setTableEditor(editor);
    }

    public TableEditorModel(ITable table, String view, boolean showFormulas) {
        this.table = table;
        this.view = view;
        this.gridTable = table.getGridTable(view);
        this.showFormulas = showFormulas;
        fullTableRegion = new GridRegion(getOriginalTable(this.gridTable).getRegion());
        displayedTableRegion = new GridRegion(gridTable.getRegion());
        othertables = new GridSplitter(gridTable.getGrid()).split();

        if (gridTable.getGrid() instanceof XlsSheetGridModel) {
            XlsSheetGridModel grid = (XlsSheetGridModel) gridTable.getGrid();
            undoGrid = new XlsUndoGrid(grid);
        }
        removeThisTable(othertables);
        makeFilteredGrid(gridTable);
    }
    
    public boolean isBusinessView() {
        return view != null && view.equalsIgnoreCase(IXlsTableNames.VIEW_BUSINESS);
    }
    
    public boolean canInsertCols(int nCols) {
        GridRegion testRegion = new GridRegion(fullTableRegion.getTop() - 1, fullTableRegion.getRight() + 1, fullTableRegion.getBottom() + 1,
                fullTableRegion.getRight() + 1 + nCols);
        for (int i = 0; i < othertables.length; i++) {
            if (IGridRegion.Tool.intersects(testRegion, othertables[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean canInsertRows(int nRows) {
        GridRegion testRegion = new GridRegion(fullTableRegion.getBottom() + 1, fullTableRegion.getLeft() - 1, fullTableRegion.getBottom() + 1
                + nRows, fullTableRegion.getRight() + 1);
        for (int i = 0; i < othertables.length; i++) {
            if (IGridRegion.Tool.intersects(testRegion, othertables[i])) {
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

    public boolean canRemoveCols(int nCols) {
        return IGridRegion.Tool.width(fullTableRegion) > nCols;
    }

    public boolean canRemoveRows(int nRows) {
        return IGridRegion.Tool.height(fullTableRegion) > nRows;
    }

    /**
     * Gets editor metadata for a specified cell
     * 
     * @param row
     * @param column
     * @return editor metadata
     */
    public Object getCellEditorMetadata(int row, int column) {
        return null;
    }

    public CellMetaInfo getCellMetaInfo(int row, int column) {

        CellMetaInfo metaInfo = IWritableGrid.Tool.getCellMetaInfo(gridTable.getGrid(), tX(column), tY(row));

        // System.out.println("Meta:" + metaInfo);
        return metaInfo;

    }

    public ICell getCell(int row, int column) {
        return gridTable.getGrid().getCell(tX(column), tY(row));
    }

    private IGridFilter getFilter(int col, int row) {
        FormattedCell fc = filteredGrid.getFormattedCell(fullTableRegion.getLeft() + col, fullTableRegion.getTop() + row);

        if (fc != null) {
            return fc.getFilter();
        }

        return null;
    }

    public void getUndoableActions(TableEditorModel other) {
        actions = other.actions;
        undoGrid = other.undoGrid;
    }

    public ITable getTable() {
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
        while (table instanceof AGridTableDelegator) {
            table = ((AGridTableDelegator) table).getOriginalGridTable();
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
        action.doAction(wgrid(), undoGrid);
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
        action.doAction(wgrid(), undoGrid);
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

        filteredGrid = new FilteredGrid(gt.getGrid(), new IGridFilter[] { new SimpleXlsFormatter() });
    }

    public synchronized void redo() {
        IUndoableAction ua = actions.getRedoAction();
        ((IUndoableGridAction) ua).doAction(wgrid(), undoGrid);
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
        action.doAction(wgrid(), undoGrid);
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
        action.doAction(wgrid(), undoGrid);
        actions.addNewAction(action);
    }

    /**
     * @param otherTables
     * 
     */
    private synchronized void removeThisTable(GridTable[] otherTables) {
        Vector<GridTable> v = new Vector<GridTable>();
        for (int i = 0; i < otherTables.length; i++) {
            if (!IGridRegion.Tool.intersects(otherTables[i], fullTableRegion)) {
                v.add(otherTables[i]);
            }
        }

        othertables = v.toArray(new GridTable[0]);
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

    public synchronized void saveAs(String fname) throws IOException {
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        xlsgrid.getSheetSource().getWorkbookSource().saveAs(fname);
    }

    public synchronized void setCellValue(int row, int col, String value) {
        IUndoableGridAction action = IWritableGrid.Tool.setStringValue(
                col, row, fullTableRegion, value, getFilter(col, row));
        action.doAction(wgrid(), undoGrid);
        actions.addNewAction(action);
    }

    public synchronized void setProperty(String name, String value) throws Exception {
        
        List<IUndoableGridAction> createdActions = new ArrayList<IUndoableGridAction>();
        int nRowsToInsert = 0;
        CellKey propertyCoordinates = IWritableGrid.Tool.getPropertyCoordinates(fullTableRegion, wgrid(), name);
        
        boolean propExists = propertyCoordinates != null;
        boolean propIsBlank = StringUtils.isBlank(value);
        
        if (propExists) {
            removeRows(1, propertyCoordinates.getRow(), propertyCoordinates.getColumn());
        }
        
        if (/*!propExists &&*/ !propIsBlank) {
            nRowsToInsert = 1;
            if (nRowsToInsert > 0 && !canInsertRows(nRowsToInsert)) {
                createdActions.add(moveTable(getUpdatedFullTable()));
            }
            GridRegionAction allTable = new GridRegionAction(fullTableRegion, ROWS, INSERT, ActionType.EXPAND,
                    nRowsToInsert);
            allTable.doAction(wgrid(), undoGrid);
            createdActions.add(allTable);
            GridRegionAction displayedTable;
            if (isBusinessView()) {
                displayedTable = new GridRegionAction(displayedTableRegion, ROWS, INSERT, ActionType.MOVE,
                        nRowsToInsert);
            } else {
                displayedTable = new GridRegionAction(displayedTableRegion, ROWS, INSERT, ActionType.EXPAND,
                        nRowsToInsert);
            }
            displayedTable.doAction(wgrid(), undoGrid);
            createdActions.add(displayedTable);
//        } 
//        else     
//        if (propExists && propIsBlank) {
//            removeRows(1, propertyCoordinates.getRow(), propertyCoordinates.getColumn());
//            return;
        }
    
        if (propIsBlank) {return;}
        
        IUndoableGridAction action = IWritableGrid.Tool
            .insertProp(fullTableRegion, displayedTableRegion, wgrid(), name, value); // returns null if set new property with empty or same value
        if (action != null) {
            action.doAction(wgrid(), undoGrid);
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
        moveTableAction.doAction(wgrid(), undoGrid);
        IUndoableGridAction changeRegions = setRegion(moveTableAction.getNewRegion());
        changeRegions.doAction(wgrid(), undoGrid);
        return new UndoableCompositeAction(moveTableAction, changeRegions);
    }

    public synchronized void setStyle(int row, int col, ICellStyle style) {
        IUndoableGridAction ua = IWritableGrid.Tool.setStyle(col, row, fullTableRegion, style);
        ua.doAction(wgrid(), undoGrid);
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
        ((IUndoableGridAction) ua).undoAction(wgrid(), undoGrid);
    }

    /**
     * Checks if cell with row/col coordinates in system of the grid returned by
     * <code>getUpdatedTable</code> methods is inside of table region.
     * 
     * @param row row number in coordinates of <code>getUpdatedTable</code>
     *            grid
     * @param col column number in coordinates of <code>getUpdatedTable</code>
     *            grid
     * @return if cell belongs to the table
     */
    public boolean updatedTableCellInsideTableRegion(int row, int col) {
        return (row >= 0 && col >= 0 && row < IGridRegion.Tool.height(fullTableRegion) && col < IGridRegion.Tool.width(fullTableRegion));
    }

    IWritableGrid wgrid() {
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

    public void setTableEditor(TableEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public TableEditor getTableEditor() {
        return tableEditor;
    }
}