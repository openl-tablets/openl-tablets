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
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
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
import org.openl.rules.table.actions.style.SetAlignmentAction;
import org.openl.rules.table.actions.style.SetFillColorAction;
import org.openl.rules.table.actions.style.SetIndentAction;
import org.openl.rules.table.actions.style.font.SetBoldAction;
import org.openl.rules.table.actions.style.font.SetItalicAction;
import org.openl.rules.table.actions.style.font.SetUnderlineAction;
import org.openl.rules.table.actions.style.font.SetColorAction;
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

    private TableEditor tableEditor;

    public TableEditorModel(TableEditor editor) {
        this(editor.getTable(), editor.getView(), editor.isShowFormulas());
        setTableEditor(editor);
    }

    public TableEditorModel(IOpenLTable table, String view, boolean showFormulas) {
        this.table = table;
        this.gridTable = table.getGridTable(view);
        if (gridTable == table.getGridTable()) { // table have no business view(e.g. Method Table)
            this.view = IXlsTableNames.VIEW_DEVELOPER;
        } else {
            this.view = view;
        }
        this.showFormulas = showFormulas;
    }

    public boolean isBusinessView() {
        return view != null && view.equalsIgnoreCase(IXlsTableNames.VIEW_BUSINESS);
    }

    public synchronized void cancel() {
        while (actions.hasUndo()) {
            undo();
        }
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
        return getOriginalGridTable().getUri();
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

    public synchronized void setCellValue(int row, int col, String value, IFormatter formatter) {
        IGridRegion originalRegion = getOriginalTableRegion();
        IFormatter dataFormatter = null;
        if (formatter != null) {
            dataFormatter = formatter;
        } else {
            ICell cell = gridTable.getGrid().getCell(originalRegion.getLeft() + col, originalRegion.getTop() + row);
            dataFormatter = cell.getDataFormatter();
        }
        IUndoableGridTableAction action = IWritableGrid.Tool.setStringValue(
                col, row, originalRegion, value, dataFormatter);
        action.doAction(gridTable);
        actions.addNewAction(action);
    }

    public synchronized void setCellValue(int row, int col, String value) {
        setCellValue(row, col, value, null);
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

    public synchronized void setAlignment(int row, int col, int alignment) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetAlignmentAction(
                region.getLeft() + col, region.getTop() + row, alignment);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setIndent(int row, int col, int indent) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetIndentAction(
                region.getLeft() + col, region.getTop() + row, indent);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFillColor(int row, int col, short[] color) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetFillColorAction(
                region.getLeft() + col, region.getTop() + row, color);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontBold(int row, int col, boolean bold) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetBoldAction(
                region.getLeft() + col, region.getTop() + row, bold);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontItalic(int row, int col, boolean italic) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetItalicAction(
                region.getLeft() + col, region.getTop() + row, italic);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontUnderline(int row, int col, boolean underlined) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetUnderlineAction(
                region.getLeft() + col, region.getTop() + row, underlined);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontColor(int row, int col, short[] color) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetColorAction(
                region.getLeft() + col, region.getTop() + row, color);
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    /**
     * @return Count of rows that is not showed.
     */
    public int getNumberOfNonShownRows() {
        return GridRegion.Tool.height(getOriginalTableRegion()) - GridRegion.Tool.height(gridTable.getRegion());
    }

    /**
     * @return Count of columns that is not showed.
     */
    public int getNumberOfNonShownCols() {
        return GridRegion.Tool.width(getOriginalTableRegion()) - GridRegion.Tool.width(gridTable.getRegion());
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