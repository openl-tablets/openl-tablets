package org.openl.rules.service;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;

public class TableServiceImpl implements TableService {

    private boolean save;

    public TableServiceImpl(boolean save) {
        this.save = save;
    }

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
            if (save) {
                sheetModel.getSheetSource().getWorkbookSource().save();
            }
        } catch (Exception e) {
            throw new TableServiceException("Could not remove the table", e);
        }
    }

    /**
     * 
     * @param table Table to copy
     * @param destSheetModel Sheet to copy the table
     * @return Region in the sheet, where table has been copied
     * @throws TableServiceException
     */
    public synchronized IGridRegion copyTable(IGridTable table, XlsSheetGridModel destSheetModel)
            throws TableServiceException {
        IGridRegion newRegion = null;
        try {
            if (destSheetModel == null) {
                destSheetModel = (XlsSheetGridModel) table.getGrid();
            }
            TableBuilder tableBuilder = new TableBuilder(destSheetModel);
            tableBuilder.beginTable(table.getWidth(), table.getHeight());
            newRegion = tableBuilder.getTableRegion();
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
            if (save) {
                tableBuilder.save();
            }
        } catch (Exception e) {
            throw new TableServiceException("Could not copy the table", e);
        }
        return newRegion;
    }

    public synchronized void copyTableTo(IGridTable table, XlsSheetGridModel destSheetModel, IGridRegion destRegion)
            throws TableServiceException {
        if (Tool.height(destRegion) != table.getHeight() || Tool.width(destRegion) != table.getWidth()) {
            throw new TableServiceException("Bad destination region size.");
        }
        try {
            if (destSheetModel == null) {
                destSheetModel = (XlsSheetGridModel) table.getGrid();
            }
            TableBuilder tableBuilder = new TableBuilder(destSheetModel);
            tableBuilder.beginTable(destRegion);
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
            if (save) {
                tableBuilder.save();
            }
        } catch (Exception e) {
            throw new TableServiceException("Could not copy the table", e);
        }
    }

    /**
     * 
     * @param table Table to move
     * @param destSheetModel Sheet to move the table
     * @return Region in the sheet, where table has been moved
     * @throws TableServiceException
     */
    public synchronized IGridRegion moveTable(IGridTable table, XlsSheetGridModel destSheetModel)
            throws TableServiceException {
        IGridRegion newRegion = null;
        try {
            newRegion = copyTable(table, destSheetModel);
            removeTable(table);
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
        return newRegion;
    }

    public synchronized void moveTableTo(IGridTable table, XlsSheetGridModel destSheetModel, IGridRegion destRegion)
            throws TableServiceException {
        try {
            copyTableTo(table, destSheetModel, destRegion);
            removeTable(table);
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
    }
}
