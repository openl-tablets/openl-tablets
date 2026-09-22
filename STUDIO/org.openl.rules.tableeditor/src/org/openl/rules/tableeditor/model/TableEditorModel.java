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
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.GridRegionAction.ActionType;
import org.openl.rules.table.actions.UndoableEditTableAction;
import org.openl.rules.table.actions.UndoableInsertColumnsAction;
import org.openl.rules.table.actions.UndoableInsertRowsAction;
import org.openl.rules.table.actions.UndoableRemoveMergedRowsAction;
import org.openl.rules.table.xls.XlsSheetGridModel;

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

    /** The column of the properties section that holds a property's name. */
    private static final int PROPERTY_NAME_COLUMN = 1;

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

        var propertyRow = GridTool.getPropertyRowIndex(fullTableRegion, gridTable.getGrid(), name);
        var propExists = propertyRow != -1;

        if (value == null) {
            if (propExists) {
                removeRows(1, propertyRow - fullTableRegion.getTop(), PROPERTY_NAME_COLUMN);
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

    private void removeRows(int nRows, int startRow, int col) {
        new UndoableRemoveMergedRowsAction(nRows, startRow, col, getMetaInfoWriter()).doAction(gridTable);
    }

    private MetaInfoWriter getMetaInfoWriter() {
        if (metaInfoWriter == null) {
            // Initialize meta info writer and use it later instead of reader
            this.metaInfoWriter = new MetaInfoWriterImpl(table.getMetaInfoReader(), gridTable);
        }
        return metaInfoWriter;
    }
}
