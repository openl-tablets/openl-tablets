/*
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.tableeditor.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriterImpl;
import org.openl.rules.table.*;
import org.openl.rules.table.actions.*;
import org.openl.rules.table.actions.GridRegionAction.ActionType;
import org.openl.rules.table.actions.style.SetAlignmentAction;
import org.openl.rules.table.actions.style.SetFillColorAction;
import org.openl.rules.table.actions.style.SetIndentAction;
import org.openl.rules.table.actions.style.font.SetBoldAction;
import org.openl.rules.table.actions.style.font.SetColorAction;
import org.openl.rules.table.actions.style.font.SetItalicAction;
import org.openl.rules.table.actions.style.font.SetUnderlineAction;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.openl.rules.tableeditor.renderkit.TableEditor;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaEnumDomain;
import org.openl.util.StringUtils;
import org.openl.util.formatters.IFormatter;

/**
 * @author snshor
 */
public class TableEditorModel {

    /**
     * Number of columns in Properties section
     */
    private static final int NUMBER_PROPERTIES_COLUMNS = 3;

    private IOpenLTable table;

    private IGridTable gridTable;
    private String view;
    private boolean showFormulas = false;
    private boolean collapseProps = false;
    private String beforeEditAction;
    private String beforeSaveAction;
    private String afterSaveAction;
    private MetaInfoWriter metaInfoWriter;

    private UndoableActions actions = new UndoableActions();

    private TableEditor tableEditor;

    public TableEditorModel(TableEditor editor) {
        this(editor.getTable(), editor.getView(), editor.isShowFormulas());
        setTableEditor(editor);
    }

    public TableEditorModel(IOpenLTable table,
            String view,
            boolean showFormulas) {
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

        gridTable.stopEditing();
        if (metaInfoWriter != null) {
            metaInfoWriter = null;
        }

        table.getMetaInfoReader().release();
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

    public IGridTable getOriginalGridTable() {
        return GridTableUtils.getOriginalTable(gridTable);
    }

    private IGridRegion getOriginalTableRegion() {
        return getOriginalGridTable().getRegion();
    }

    public synchronized void insertColumns(int nCols, int beforeCol, int row) {
        IUndoableGridTableAction insertColumnsAction = new UndoableInsertColumnsAction(nCols, beforeCol, row,
                getMetaInfoWriter());
        insertColumnsAction.doAction(gridTable);
        actions.addNewAction(insertColumnsAction);
    }

    public synchronized void insertRows(int nRows, int beforeRow, int col) {
        IUndoableGridTableAction insertRowsAction = new UndoableInsertRowsAction(nRows, beforeRow, col,
                getMetaInfoWriter());
        insertRowsAction.doAction(gridTable);
        actions.addNewAction(insertRowsAction);
    }

    public synchronized void redo() {
        IUndoableAction ua = actions.getRedoAction();

        ((IUndoableGridTableAction) ua).doAction(gridTable);
    }

    public synchronized void removeRows(int nRows, int startRow, int col) {
        IUndoableGridTableAction removeRowsAction = new UndoableRemoveRowsAction(nRows, startRow, col,
                getMetaInfoWriter());
        removeRowsAction.doAction(gridTable);
        actions.addNewAction(removeRowsAction);
    }

    public synchronized void removeColumns(int nCols, int startCol, int row) {
        IUndoableGridTableAction removeColumnsAction = new UndoableRemoveColumnsAction(nCols, startCol, row,
                getMetaInfoWriter());
        removeColumnsAction.doAction(gridTable);
        actions.addNewAction(removeColumnsAction);
    }

    /**
     * @return New table id on the sheet where it was saved. It is needed for tables that were moved
     * to new place during adding new rows and columns on editing. We need to know new destination of the table.
     */
    public synchronized String save() throws IOException {
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        xlsgrid.getSheetSource().getWorkbookSource().save();
        actions = new UndoableActions();
        String uri = getOriginalGridTable().getUri();
        return TableUtils.makeTableId(uri);
    }

    /**
     * @return Sheet source of editable table
     */
    public XlsSheetSourceCodeModule getSheetSource() {
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        return xlsgrid.getSheetSource();
    }

    public synchronized void saveAs(String fname) throws IOException {
        getSheetSource().getWorkbookSource().saveAs(fname);
    }

    public synchronized void setCellValue(int row, int col, String value, IFormatter formatter) {
        IGridRegion originalRegion = getOriginalTableRegion();
        int gcol = originalRegion.getLeft() + col;
        int grow = originalRegion.getTop() + row;

        IFormatter dataFormatter;
        if (formatter != null) {
            dataFormatter = formatter;
        } else {
            ICell cell = gridTable.getGrid().getCell(gcol, grow);
            CellMetaInfo metaInfo = getMetaInfoReader().getMetaInfo(grow, gcol);
            dataFormatter = XlsDataFormatterFactory.getFormatter(cell, metaInfo);

            // Don't reformat value if value is belong to domain
            IOpenClass dataType = metaInfo == null ? null : metaInfo.getDataType();
            if (value != null && dataType != null) {
                IDomain<?> domain = dataType.getDomain();
                if (domain instanceof EnumDomain || domain instanceof JavaEnumDomain) {
                    for (Object domainValue : domain) {
                        if (value.equals(domainValue)) {
                            // Found exact match. Don't use formatter.
                            // This is needed to support the case: In domain exists value 1.230 but formatter tries
                            // to write it as 1.23 so we write incorrect domain value when use formatter.
                            dataFormatter = null;
                            break;
                        }
                    }
                }
            }
        }

        Object result;
        if (dataFormatter != null) {
            result = dataFormatter.parse(value);
        } else {
            result = value;
        }

        IUndoableGridTableAction action = new UndoableSetValueAction(gcol, grow, result, getMetaInfoWriter());

        action.doAction(gridTable);
        actions.addNewAction(action);
    }

    public MetaInfoReader getMetaInfoReader() {
        // If metaInfoWriter isn't null, then currently we edit the table, so we must return metaInfoWriter to include
        // user changes when display the table to a user.
        return metaInfoWriter != null ? metaInfoWriter : table.getMetaInfoReader();
    }

    public synchronized void setCellValue(int row, int col, String value) {
        setCellValue(row, col, value, null);
    }

    public synchronized void setProperty(String name, Object value) throws Exception {
        List<IUndoableGridTableAction> createdActions = new ArrayList<>();
        int nColsToInsert = 0;

        IGridTable fullTable = getOriginalGridTable();
        IGridRegion fullTableRegion = fullTable.getRegion();

        CellKey propertyCoordinates = getPropertyCoordinates(fullTableRegion, gridTable.getGrid(), name);

        boolean propExists = propertyCoordinates != null;
        boolean propIsBlank = value == null;

        if (propIsBlank) {
            if (propExists) {
                removeRows(1, propertyCoordinates.getRow(), propertyCoordinates.getColumn());
            }
            return;
        }
        if (!propExists) {
            int tableWidth = fullTable.getWidth();
            if (tableWidth < NUMBER_PROPERTIES_COLUMNS) {
                nColsToInsert = NUMBER_PROPERTIES_COLUMNS - tableWidth;
            }
            if (!UndoableInsertRowsAction.canInsertRows(gridTable, 1)
                    || !UndoableInsertColumnsAction.canInsertColumns(gridTable, nColsToInsert)) {
                createdActions.add(UndoableEditTableAction.moveTable(fullTable));
            }
            GridRegionAction allTable = new GridRegionAction(fullTableRegion, UndoableEditTableAction.ROWS,
                    UndoableEditTableAction.INSERT, ActionType.EXPAND, 1);
            allTable.doAction(gridTable);
            createdActions.add(allTable);
            if (isBusinessView()) {
                GridRegionAction displayedTable = new GridRegionAction(gridTable.getRegion(),
                        UndoableEditTableAction.ROWS, UndoableEditTableAction.INSERT, ActionType.MOVE, 1);
                displayedTable.doAction(gridTable);
                createdActions.add(displayedTable);
            }
        }

        IUndoableGridTableAction action = GridTool.insertProp(fullTableRegion, gridTable.getGrid(), name, value,
                getMetaInfoWriter());
        if (action != null) {
            action.doAction(gridTable);
            createdActions.add(action);
        }
        if (!createdActions.isEmpty()) {
            actions.addNewAction(new UndoableCompositeAction(createdActions));
        }
    }

    /**
     * Checks if the table specified by its region contains property.
     */
    private CellKey getPropertyCoordinates(IGridRegion region, IGrid grid, String propName) {
        int left = region.getLeft();
        int top = region.getTop();

        ICell propsHeaderCell = grid.getCell(left, top + 1);
        String propsHeader = propsHeaderCell.getStringValue();
        if (propsHeader == null || !propsHeader.equals(PropertiesHelper.PROPERTIES_HEADER)) {
            // There is no properties
            return null;
        }
        int propsCount = propsHeaderCell.getHeight();

        for (int i = 0; i < propsCount; i++) {
            ICell propNameCell = grid.getCell(left + propsHeaderCell.getWidth(), top + 1 + i);
            String pName = propNameCell.getStringValue();

            if (pName != null && pName.equals(propName)) {
                return CellKey.CellKeyFactory.getCellKey(1, 1 + i);
            }
        }

        return null;
    }

    public synchronized void setProperty(String name, String value) throws Exception {
        Object objectValue = null;
        if (StringUtils.isNotBlank(value)) {
            TablePropertyDefinition tablePropeprtyDefinition = TablePropertyDefinitionUtils.getPropertyByName(name);
            if (tablePropeprtyDefinition != null) {
                Class<?> type = tablePropeprtyDefinition.getType().getInstanceClass();
                IFormatter formatter = FormattersManager.getFormatter(type, tablePropeprtyDefinition.getFormat());
                objectValue = formatter.parse(value);
            } else {
                objectValue = value;
            }
        }
        setProperty(name, objectValue);
    }

    public synchronized void removeProperty(String name) throws Exception {
        setProperty(name, (Object) null);
    }

    public synchronized void setAlignment(int row, int col, HorizontalAlignment alignment) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetAlignmentAction(
                region.getLeft() + col, region.getTop() + row, alignment, getMetaInfoWriter());
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setIndent(int row, int col, int indent) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetIndentAction(
                region.getLeft() + col, region.getTop() + row, indent, getMetaInfoWriter());
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFillColor(int row, int col, short[] color) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetFillColorAction(
                region.getLeft() + col, region.getTop() + row, color, getMetaInfoWriter());
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontBold(int row, int col, boolean bold) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetBoldAction(
                region.getLeft() + col, region.getTop() + row, bold, getMetaInfoWriter());
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontItalic(int row, int col, boolean italic) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetItalicAction(
                region.getLeft() + col, region.getTop() + row, italic, getMetaInfoWriter());
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontUnderline(int row, int col, boolean underlined) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetUnderlineAction(
                region.getLeft() + col, region.getTop() + row, underlined, getMetaInfoWriter());
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    public synchronized void setFontColor(int row, int col, short[] color) {
        IGridRegion region = getOriginalTableRegion();
        IUndoableGridTableAction ua = new SetColorAction(
                region.getLeft() + col, region.getTop() + row, color, getMetaInfoWriter());
        ua.doAction(gridTable);
        actions.addNewAction(ua);
    }

    private MetaInfoWriter getMetaInfoWriter() {
        if (metaInfoWriter == null) {
            // Initialize meta info writer and use it later instead of reader
            this.metaInfoWriter = new MetaInfoWriterImpl(table.getMetaInfoReader(), gridTable);
        }
        return metaInfoWriter;
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

    public String getBeforeEditAction() {
        return beforeEditAction;
    }

    public void setBeforeEditAction(String beforeEditAction) {
        this.beforeEditAction = beforeEditAction;
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