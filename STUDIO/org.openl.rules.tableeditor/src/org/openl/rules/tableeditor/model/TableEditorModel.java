/*
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.tableeditor.model;

import java.io.IOException;
import java.util.ArrayList;

import lombok.Getter;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriterImpl;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.GridRegionAction.ActionType;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.actions.UndoableActions;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableEditTableAction;
import org.openl.rules.table.actions.UndoableInsertColumnsAction;
import org.openl.rules.table.actions.UndoableInsertRowsAction;
import org.openl.rules.table.actions.UndoableRemoveMergedRowsAction;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.StringUtils;

/**
 * Writes a table's properties section.
 *
 * <p>A property is set in place: the section grows a row where one is needed, and the table is moved when it has no
 * room to grow. Setting a property to {@code null} takes its row away.
 *
 * @author snshor
 */
public class TableEditorModel {

    /**
     * Number of columns in Properties section
     */
    private static final int NUMBER_PROPERTIES_COLUMNS = 3;

    @Getter
    private final IOpenLTable table;

    @Getter
    private final IGridTable gridTable;
    private final String view;
    private MetaInfoWriter metaInfoWriter;

    private UndoableActions actions = new UndoableActions();

    public TableEditorModel(IOpenLTable table, String view) {
        this.table = table;
        this.gridTable = table.getGridTable(view);
        if (gridTable == table.getGridTable()) { // table have no business view(e.g. Method Table)
            this.view = IXlsTableNames.VIEW_DEVELOPER;
        } else {
            this.view = view;
        }
    }

    public boolean isBusinessView() {
        return view != null && view.equalsIgnoreCase(IXlsTableNames.VIEW_BUSINESS);
    }

    public IGridTable getOriginalGridTable() {
        return GridTableUtils.getOriginalTable(gridTable);
    }

    /**
     * @return New table id on the sheet where it was saved. It is needed for tables that were moved to new place during
     * adding new rows and columns on editing. We need to know new destination of the table.
     */
    public synchronized String save() throws IOException {
        var xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        xlsgrid.getSheetSource().getWorkbookSource().save();
        actions = new UndoableActions();
        var uri = getOriginalGridTable().getUri();
        return TableUtils.makeTableId(uri);
    }

    public synchronized void setProperty(String name, Object value) {
        var createdActions = new ArrayList<IUndoableGridTableAction>();

        var fullTable = getOriginalGridTable();
        var fullTableRegion = fullTable.getRegion();

        var propertyCoordinates = getPropertyCoordinates(fullTableRegion, gridTable.getGrid(), name);

        var propExists = propertyCoordinates != null;
        var propIsBlank = value == null;

        if (propIsBlank) {
            if (propExists) {
                removeRows(1, propertyCoordinates.getRow(), propertyCoordinates.getColumn());
            }
            return;
        }

        // Initialize meta info writer before any modifications
        var metaInfoWriter = getMetaInfoWriter();

        if (!propExists) {
            var tableWidth = fullTable.getWidth();
            var nColsToInsert = 0;
            if (tableWidth < NUMBER_PROPERTIES_COLUMNS) {
                nColsToInsert = NUMBER_PROPERTIES_COLUMNS - tableWidth;
            }
            if (!UndoableInsertRowsAction.canInsertRows(gridTable, 1) || !UndoableInsertColumnsAction
                    .canInsertColumns(gridTable, nColsToInsert)) {
                createdActions.add(UndoableEditTableAction.moveTable(fullTable, metaInfoWriter));
            }
            var allTable = new GridRegionAction(fullTableRegion,
                    UndoableEditTableAction.ROWS,
                    UndoableEditTableAction.INSERT,
                    ActionType.EXPAND,
                    1);
            allTable.doAction(gridTable);
            createdActions.add(allTable);
            if (isBusinessView()) {
                var displayedTable = new GridRegionAction(gridTable
                        .getRegion(), UndoableEditTableAction.ROWS, UndoableEditTableAction.INSERT, ActionType.MOVE, 1);
                displayedTable.doAction(gridTable);
                createdActions.add(displayedTable);
            }
        }

        IUndoableGridTableAction action = GridTool
                .insertProp(fullTableRegion, gridTable.getGrid(), name, value, metaInfoWriter);
        if (action != null) {
            action.doAction(gridTable);
            createdActions.add(action);
        }
        if (!createdActions.isEmpty()) {
            actions.addNewAction(new UndoableCompositeAction(createdActions));
        }
    }

    public synchronized void setProperty(String name, String value) {
        Object objectValue = null;
        if (StringUtils.isNotBlank(value)) {
            var tablePropeprtyDefinition = TablePropertyDefinitionUtils.getPropertyByName(name);
            if (tablePropeprtyDefinition != null) {
                Class<?> type = tablePropeprtyDefinition.getType().getInstanceClass();
                var formatter = FormattersManager.getFormatter(type, tablePropeprtyDefinition.getFormat());
                objectValue = formatter.parse(value);
            } else {
                objectValue = value;
            }
        }
        setProperty(name, objectValue);
    }

    private synchronized void removeRows(int nRows, int startRow, int col) {
        var removeRowsAction = new UndoableRemoveMergedRowsAction(nRows,
                startRow,
                col,
                getMetaInfoWriter());
        removeRowsAction.doAction(gridTable);
        actions.addNewAction(removeRowsAction);
    }

    /**
     * Checks if the table specified by its region contains property.
     */
    private CellKey getPropertyCoordinates(IGridRegion region, IGrid grid, String propName) {
        var left = region.getLeft();
        var top = region.getTop();

        var propsHeaderCell = grid.getCell(left, top + 1);
        var propsHeader = propsHeaderCell.getStringValue();
        if (propsHeader == null || !propsHeader.equals(PropertiesHelper.PROPERTIES_HEADER)) {
            // There is no properties
            return null;
        }
        var propsCount = propsHeaderCell.getHeight();

        for (var i = 0; i < propsCount; i++) {
            var propNameCell = grid.getCell(left + propsHeaderCell.getWidth(), top + 1 + i);
            var pName = propNameCell.getStringValue();

            if (pName != null && pName.equals(propName)) {
                return CellKey.CellKeyFactory.getCellKey(1, 1 + i);
            }
        }

        return null;
    }

    private MetaInfoWriter getMetaInfoWriter() {
        if (metaInfoWriter == null) {
            // Initialize meta info writer and use it later instead of reader
            this.metaInfoWriter = new MetaInfoWriterImpl(table.getMetaInfoReader(), gridTable);
        }
        return metaInfoWriter;
    }
}
