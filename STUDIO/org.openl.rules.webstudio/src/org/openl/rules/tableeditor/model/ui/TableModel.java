package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;

/**
 * A table as it is laid out: every place of its grid, with the spans, the style and the borders of the cell that
 * stands there.
 */
public class TableModel {

    private final ICellModel[][] cells;

    private final IGridTable gridTable;

    private final int height;

    /**
     * Lays out a table.
     *
     * @param table          the table to lay out
     * @param numRows        how many rows from the top to lay out, or {@code -1} for all of them
     * @param metaInfoReader what the compiler knows about the table's cells
     * @return the laid out table, or {@code null} when there is no table
     */
    public static TableModel initializeTableModel(IGridTable table, int numRows, MetaInfoReader metaInfoReader) {
        if (table == null) {
            return null;
        }
        var region = table.getRegion();
        if (numRows > -1 && region.getTop() + numRows < region.getBottom()) {
            region = new GridRegion(region);
            ((GridRegion) region).setBottom(region.getTop() + numRows - 1);
        }
        return new TableViewer(table.getGrid(), region, metaInfoReader).buildModel(table);
    }

    TableModel(int width, int height, IGridTable gridTable) {
        this.height = height;
        this.cells = new ICellModel[height][];
        for (var i = 0; i < cells.length; i++) {
            cells[i] = new ICellModel[width];
        }

        this.gridTable = gridTable;
    }

    void addCell(ICellModel cm, int row, int column) {
        if (row < cells.length && column < cells[row].length) {
            cells[row][column] = cm;
        }
    }

    CellModel findCellModel(int col, int row, int border) {
        if (col < 0 || row < 0 || row >= cells.length || col >= cells[0].length) {
            return null;
        }

        var icm = cells[row][col];
        if (icm == null) {
            return null;
        }
        return switch (border) {
            case ICellStyle.TOP -> {
                if (icm instanceof CellModel model) {
                    yield model;
                }
                var cm = ((CellModelDelegator) icm).getModel();
                yield cm.getRow() == row ? cm : null;
            }
            case ICellStyle.LEFT -> {
                if (icm instanceof CellModel model) {
                    yield model;
                }
                var cm = ((CellModelDelegator) icm).getModel();
                yield cm.getColumn() == col ? cm : null;
            }
            case ICellStyle.RIGHT -> {
                CellModel cm;
                if (icm instanceof CellModel model) {
                    cm = model;
                    yield cm.getColspan() == 1 ? cm : null;
                }
                cm = ((CellModelDelegator) icm).getModel();
                yield cm.getColumn() + cm.getColspan() - 1 == col ? cm : null;
            }
            case ICellStyle.BOTTOM -> {
                CellModel cm;
                if (icm instanceof CellModel model) {
                    cm = model;
                    yield cm.getRowspan() == 1 ? cm : null;
                }
                cm = ((CellModelDelegator) icm).getModel();
                yield cm.getRow() + cm.getRowspan() - 1 == row ? cm : null;
            }
            default -> throw new IllegalArgumentException("Incorrect border");
        };

    }

    /**
     * Cells property getter
     *
     * @return cells
     */
    public ICellModel[][] getCells() {
        return cells;
    }

    public IGridTable getGridTable() {
        return gridTable;
    }

    boolean hasCell(int r, int c) {
        // This is the correct case if we add empty rows to the display of the table,
        // in place of the deleted ones, when comparing tables.
        if (cells.length <= r || cells[0].length <= c) {
            return false;
        }
        return cells[r][c] != null;
    }

    public int getHeight() {
        return height;
    }
}
