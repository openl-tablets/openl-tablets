package org.openl.rules.service;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;

public class TableServiceImpl {

    public synchronized void removeTable(IGridTable table) throws TableServiceException {
        try {
            IGridRegion tableRegion = table.getRegion();
            int left = tableRegion.getLeft();
            int top = tableRegion.getTop();
            XlsSheetGridModel sheetModel = (XlsSheetGridModel) table.getGrid();
            for (int i = 0; i < table.getWidth(); i++) {
                for (int j = 0; j < table.getHeight(); j++) {
                    ICell cell = table.getCell(i, j);
                    if (cell.getWidth() != 1 || cell.getHeight() != 1) {
                        sheetModel.removeMergedRegion(left + i, top + j);
                    }
                    sheetModel.clearCell(left + i, top + j);
                }
            }
        } catch (Exception e) {
            throw new TableServiceException("Could not remove the table", e);
        }
    }

    /**
     * @param table Table to copy
     * @return Region in the sheet, where table has been copied
     * @throws TableServiceException
     */
    private IGridRegion copyTable(IGridTable table)
            throws TableServiceException {
        IGridRegion newRegion;
        try {
            TableBuilder tableBuilder = new TableBuilder((XlsSheetGridModel) table.getGrid());
            tableBuilder.beginTable(table.getWidth(), table.getHeight());
            newRegion = tableBuilder.getTableRegion();
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
        } catch (Exception e) {
            throw new TableServiceException("Could not copy the table", e);
        }
        return newRegion;
    }

    private void copyTableTo(IGridTable table, IGridRegion destRegion)
            throws TableServiceException {
        if (Tool.height(destRegion) != table.getHeight() || Tool.width(destRegion) != table.getWidth()) {
            throw new TableServiceException("Bad destination region size.");
        }
        try {
            TableBuilder tableBuilder = new TableBuilder((XlsSheetGridModel) table.getGrid());
            tableBuilder.beginTable(destRegion);
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
        } catch (Exception e) {
            throw new TableServiceException("Could not copy the table", e);
        }
    }

    /**
     * @param table Table to move
     * @return Region in the sheet, where table has been moved
     * @throws TableServiceException
     */
    public synchronized IGridRegion moveTable(IGridTable table)
            throws TableServiceException {
        IGridRegion newRegion;
        try {
            newRegion = copyTable(table);
            removeTable(table);
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
        return newRegion;
    }

    public synchronized void moveTableTo(IGridTable table, IGridRegion destRegion)
            throws TableServiceException {
        try {
            copyTableTo(table, destRegion);
            removeTable(table);
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
    }
}
