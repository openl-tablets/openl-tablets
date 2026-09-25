package org.openl.studio.projects.service.tables.read;

import org.openl.rules.tableeditor.model.ui.CellModel;

/**
 * The cells a merged one reaches over, in a table read as a matrix.
 *
 * <p>A cell inside a merged region that is not the one it starts at holds nothing of its own: the grid model
 * stands the merged cell in for it rather than describing it. A read that took such a cell for a cell of its
 * own would answer one group of the workbook as several.
 */
final class CoveredCells {

    private final boolean[][] covered;
    private final int height;
    private final int width;

    /**
     * @param height how many rows the matrix holds
     * @param width  how many columns it holds
     */
    CoveredCells(int height, int width) {
        this.height = height;
        this.width = width;
        this.covered = new boolean[height][width];
    }

    /** Whether a merged cell read earlier reaches over this one. */
    boolean holds(int row, int column) {
        return covered[row][column];
    }

    /** Notes the cells the merged one at this place reaches over, itself apart. */
    void mark(int row, int column, CellModel cellModel) {
        var lastRow = Math.min(row + cellModel.getRowspan(), height);
        var lastColumn = Math.min(column + cellModel.getColspan(), width);
        for (var r = row; r < lastRow; r++) {
            for (var c = column; c < lastColumn; c++) {
                if (r > row || c > column) {
                    covered[r][c] = true;
                }
            }
        }
    }
}
