/*
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.tableeditor.model;

import java.io.IOException;

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
 * <p>The table is always written as the developer sees it, with its properties section in view.
 *
 * @author snshor
 */
public class TableEditorModel {

    /**
     * Number of columns in Properties section
     */
    private static final int NUMBER_PROPERTIES_COLUMNS = 3;

    private final IOpenLTable table;
    private final IGridTable gridTable;
    private MetaInfoWriter metaInfoWriter;

    public TableEditorModel(IOpenLTable table) {
        this.table = table;
        this.gridTable = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);
    }

    private IGridTable getOriginalGridTable() {
        return GridTableUtils.getOriginalTable(gridTable);
    }

    /**
     * @return New table id on the sheet where it was saved. It is needed for tables that were moved to new place during
     * adding new rows and columns on editing. We need to know new destination of the table.
     */
    public String save() throws IOException {
        var xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        xlsgrid.getSheetSource().getWorkbookSource().save();
        var uri = getOriginalGridTable().getUri();
        return TableUtils.makeTableId(uri);
    }

    public void setProperty(String name, Object value) {
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
                UndoableEditTableAction.moveTable(fullTable, metaInfoWriter);
            }
            new GridRegionAction(fullTableRegion,
                    UndoableEditTableAction.ROWS,
                    UndoableEditTableAction.INSERT,
                    ActionType.EXPAND,
                    1).doAction(gridTable);
        }

        var action = GridTool.insertProp(fullTableRegion, gridTable.getGrid(), name, value, metaInfoWriter);
        if (action != null) {
            action.doAction(gridTable);
        }
    }

    public void setProperty(String name, String value) {
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

    private void removeRows(int nRows, int startRow, int col) {
        new UndoableRemoveMergedRowsAction(nRows, startRow, col, getMetaInfoWriter()).doAction(gridTable);
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
