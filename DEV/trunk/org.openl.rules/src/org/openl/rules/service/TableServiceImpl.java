package org.openl.rules.service;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;

public class TableServiceImpl implements TableService {

    public synchronized void removeTable(IGridTable table) throws TableServiceException {
        try {
            IGridRegion tableRegion = table.getRegion();
            int left = tableRegion.getLeft();
            int top = tableRegion.getTop();
            XlsSheetGridModel sheetModel = (XlsSheetGridModel) table.getGrid();
            for (int i = 0; i < table.getGridWidth(); i++) {
                for (int j = 0; j < table.getGridHeight(); j++) {
                    ICell cell = table.getCell(i, j);
                    if (cell.getCellWidth() != 1 || cell.getCellHeight() != 1) {
                        sheetModel.removeMergedRegion(left + i, top + j);
                    }
                    sheetModel.clearCell(left + i, top + j);
                }
            }
            sheetModel.getSheetSource().getWorkbookSource().save();
        } catch (Exception e) {
            throw new TableServiceException("Could not remove the table", e);
        }
    }

    public synchronized void copyTable(IGridTable table, XlsSheetGridModel destSheetModel)
            throws TableServiceException {
        try {
            if (destSheetModel == null) {
                destSheetModel = (XlsSheetGridModel) table.getGrid();
            }
            TableBuilder tableBuilder = new TableBuilder(destSheetModel);
            tableBuilder.beginTable(table.getGridWidth(), table.getGridHeight());
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
        } catch (Exception e) {
            throw new TableServiceException("Could not copy the table", e);
        }
    }

    public synchronized void moveTable(IGridTable table, XlsSheetGridModel destSheetModel)
            throws TableServiceException {
        try {
            if (destSheetModel == null) {
                destSheetModel = (XlsSheetGridModel) table.getGrid();
            }
            copyTable(table, destSheetModel);
            removeTable(table);
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
    }

}
