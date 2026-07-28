package org.openl.rules.service;

import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridRegion.Tool;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;

@RequiredArgsConstructor
public class TableServiceImpl {
    private final MetaInfoWriter metaInfoWriter;

    public TableServiceImpl() {
        this(null);
    }

    public synchronized void removeTable(IGridTable table) throws TableServiceException {
        try {
            var tableRegion = table.getRegion();
            var left = tableRegion.getLeft();
            var top = tableRegion.getTop();
            var right = tableRegion.getRight();
            var bottom = tableRegion.getBottom();
            var sheetModel = (XlsSheetGridModel) table.getGrid();
            for (var row = top; row <= bottom; row++) {
                for (var col = left; col <= right; col++) {
                    var cell = sheetModel.getCell(col, row);
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
            var tableBuilder = new TableBuilder((XlsSheetGridModel) table.getGrid(), metaInfoWriter);
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
            var tableBuilder = new TableBuilder((XlsSheetGridModel) table.getGrid(), metaInfoWriter);
            tableBuilder.beginTable(destRegion);
            tableBuilder.writeGridTable(table);
            tableBuilder.endTable();
        } catch (Exception e) {
            throw new TableServiceException("Could not move the table", e);
        }
        removeTable(table);
    }
}
