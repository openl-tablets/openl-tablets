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
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.IUndoableAction;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.actions.UndoableActions;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableEditTableAction;
import org.openl.rules.table.actions.UndoableInsertColumnsAction;
import org.openl.rules.table.actions.UndoableInsertRowsAction;
import org.openl.rules.table.actions.UndoableRemoveColumnsAction;
import org.openl.rules.table.actions.UndoableRemoveRowsAction;
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

/**
 * @author snshor
 */
public class TableEditorModel {

    /** Number of columns in Properties section */
    public static final int NUMBER_PROPERTIES_COLUMNS = 3;

    private IOpenLTable table;

    private IGridTable gridTable;
    private String view;
    private boolean showFormulas = false;
    private boolean collapseProps = false;
    private String beforeSaveAction;
    private String afterSaveAction;
    private String saveFailureAction;

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

        makeFilteredGrid(gridTable);
    }

    public boolean isBusinessView() {
        return view != null && view.equalsIgnoreCase(IXlsTableNames.VIEW_BUSINESS);
    }

    public synchronized void cancel() {
        while (actions.hasUndo()) {
            undo();
        }
    }

    private IFormatter getFormatter(int col, int row) {
        IFormatter formatter = null;
        IGridRegion originalRegion = getOriginalTableRegion();
        FormattedCell fc = filteredGrid.getFormattedCell(
                originalRegion.getLeft() + col, originalRegion.getTop() + row);

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

    public IGridTable getGridTable() {
        return gridTable;
    }

    public synchronized boolean hasRedo() {
        return actions.hasRedo();
    }

    public synchronized boolean hasUndo() {
        return actions.hasUndo();
    }

    /**
     * Extracts original grid table.
     * 
     * @param table Table which we have.
     * @return Original table that includes our table.
     */
    public IGridTable getOriginalGridTable() {
        IGridTable resultTable = gridTable;
        while (resultTable instanceof AGridTableDecorator) {
            resultTable = ((AGridTableDecorator) resultTable).getOriginalGridTable();
        }
        return resultTable;
    }

    public IGridRegion getOriginalTableRegion() {
        return getOriginalGridTable().getRegion();
    }

    public synchronized void insertColumns(int nCols, int beforeCol, int row) throws Exception {
        IUndoableGridTableAction insertColumnsAction = new UndoableInsertColumnsAction(nCols, beforeCol, row);
        insertColumnsAction.doAction(gridTable);
        actions.addNewAction(insertColumnsAction);
    }

    public synchronized void insertRows(int nRows, int beforeRow, int col) throws Exception {
        IUndoableGridTableAction insertRowsAction = new UndoableInsertRowsAction(nRows, beforeRow, col);
        insertRowsAction.doAction(gridTable);
        actions.addNewAction(insertRowsAction);
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

        ((IUndoableGridTableAction) ua).doAction(gridTable);
    }

    public synchronized void removeRows(int nRows, int startRow, int col) {
        IUndoableGridTableAction removeRowsAction = new UndoableRemoveRowsAction(nRows, startRow, col);
        removeRowsAction.doAction(gridTable);
        actions.addNewAction(removeRowsAction);
    }

    public synchronized void removeColumns(int nCols, int startCol, int row) {
        IUndoableGridTableAction removeColumnsAction = new UndoableRemoveColumnsAction(nCols, startCol, row);
        removeColumnsAction.doAction(gridTable);
        actions.addNewAction(removeColumnsAction);
    }

    /**     
     * @return New table URI on the sheet where it was saved. It is needed for tables that were moved
     * to new place during adding new rows and columns on editing. We need to know new destination of the table.
     * @throws IOException
     */
    public synchronized String save() throws IOException {
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        xlsgrid.getSheetSource().getWorkbookSource().save();
        actions = new UndoableActions();
        return gridTable.getUri();
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
        IUndoableGridTableAction action = IWritableGrid.Tool.setStringValue(
                col, row, getOriginalTableRegion(), value, getFormatter(col, row));
        action.doAction(gridTable);
        actions.addNewAction(action);
    }

    public synchronized void setProperty(String name, String value) throws Exception {
        List<IUndoableGridTableAction> createdActions = new ArrayList<IUndoableGridTableAction>();
        int nRowsToInsert = 0;
        int nColsToInsert = 0;

        IGridTable fullTable = getOriginalGridTable();
        IGridRegion fullTableRegion = fullTable.getRegion();

        CellKey propertyCoordinates = IWritableGrid.Tool.getPropertyCoordinates(fullTableRegion, gridTable, name);

        boolean propExists = propertyCoordinates != null;
        boolean propIsBlank = StringUtils.isBlank(value);

        if (!propIsBlank && !propExists) {
            int tableWidth = fullTable.getWidth();
            if (tableWidth < NUMBER_PROPERTIES_COLUMNS) {
                nColsToInsert = NUMBER_PROPERTIES_COLUMNS - tableWidth;
            }
            nRowsToInsert = 1;
            if ((nRowsToInsert > 0 && !UndoableInsertRowsAction.canInsertRows(gridTable, nRowsToInsert))
                    || !UndoableInsertColumnsAction.canInsertColumns(gridTable, nColsToInsert)) {
                createdActions.add(UndoableEditTableAction.moveTable(fullTable));
            }
            GridRegionAction allTable = new GridRegionAction(fullTableRegion, UndoableEditTableAction.ROWS,
                    UndoableEditTableAction.INSERT, ActionType.EXPAND, nRowsToInsert);
            allTable.doAction(gridTable);
            createdActions.add(allTable);
            if (isBusinessView()) {
                GridRegionAction displayedTable = new GridRegionAction(gridTable.getRegion(),
                        UndoableEditTableAction.ROWS, UndoableEditTableAction.INSERT, ActionType.MOVE, nRowsToInsert);
                displayedTable.doAction(gridTable);
                createdActions.add(displayedTable);
            }
        } else if (propIsBlank && propExists) {
            removeRows(1, propertyCoordinates.getRow(), propertyCoordinates.getColumn());
            return;
        }

        IUndoableGridTableAction action = IWritableGrid.Tool.insertProp(fullTableRegion, gridTable, name, value);
        if (action != null) {
            action.doAction(gridTable);
            createdActions.add(action);
        }
        if (!createdActions.isEmpty()) {
            actions.addNewAction(new UndoableCompositeAction(createdActions));
        }
    }

    public synchronized void setStyle(int row, int col, ICellStyle style) {
        IUndoableGridTableAction ua = IWritableGrid.Tool.setStyle(col, row, getOriginalTableRegion(), style);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    /**
     * @return Count of rows that is not showed.
     */
    public int getNumberOfNonShownRows() {
        return getOriginalGridTable().getHeight() - gridTable.getHeight();
    }

    /**
     * @return Count of columns that is not showed.
     */
    public int getNumberOfNonShownCols() {
        return getOriginalGridTable().getWidth() - gridTable.getWidth();
    }

    public synchronized void undo() {
        IUndoableAction ua = actions.getUndoAction();

        ((IUndoableGridTableAction) ua).undoAction(gridTable);
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