package org.openl.rules.service;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;

public class TableServiceImpl {
    private MetaInfoWriter metaInfoWriter;

    public TableServiceImpl() {
        this(null);
    }

    public TableServiceImpl(MetaInfoWriter metaInfoWriter) {
        this.metaInfoWriter = metaInfoWriter;
    }

    public synchronized void removeTable(IGridTable table) throws TableServiceException {
        try {
            IGridRegion tableRegion = table.getRegion();
            int left = tableRegion.getLeft();
            int top = tableRegion.getTop();
            int right = tableRegion.getRight();
            int bottom = tableRegion.getBottom();
            XlsSheetGridModel sheetModel = (XlsSheetGridModel) table.getGrid();
            for (int row = top; row <= bottom; row++) {
                for (int col = left; col <= right; col++) {
                    ICell cell = sheetModel.getCell(col, row);
                    if (cell.getWidth() != 1 || cell.getHeight() != 1) {
                        sheetModel.removeMergedRegion(col, row);
                    }
                    sheetModel.clearCell(col, row);
                }
            }
        } catch (Exception e) {
            throw new TableServiceException("Could not remove the table", e);
        }
    }

    /**
     * @param table Table to move
     * @return Region in the sheet, where table has been moved
     */
    public synchronized IGridRegion moveTable(IGridTable table) throws TableServiceException {
        IGridRegion newRegion;
        try {
            TableBuilder tableBuilder = new TableBuilder((XlsSheetGridModel) table.getGrid(), metaInfoWriter);
            tableBuilder.beginTable(table.getWidth(), table.getHeight());
            newRegion = tableBuilder.getTableRegion();
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
        removeTable(table);
        return newRegion;
    }

    public synchronized void moveTableTo(IGridTable table, IGridRegion destRegion) throws TableServiceException {
        if (Tool.height(destRegion) != table.getHeight() || Tool.width(destRegion) != table.getWidth()) {
            throw new TableServiceException("Bad destination region size.");
        }
        try {
            TableBuilder tableBuilder = new TableBuilder((XlsSheetGridModel) table.getGrid(), metaInfoWriter);
            tableBuilder.beginTable(destRegion);
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
        removeTable(table);
    }
}
