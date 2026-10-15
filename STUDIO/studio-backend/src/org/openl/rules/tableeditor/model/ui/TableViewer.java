package org.openl.rules.tableeditor.model.ui;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;

/** Lays a table's region out place by place, so that every place of the grid says what stands there. */
@RequiredArgsConstructor
class TableViewer {

    /** The sides of a cell, in the order a style names their borders. */
    private static final int[] SIDES = {ICellStyle.TOP, ICellStyle.RIGHT, ICellStyle.BOTTOM, ICellStyle.LEFT};

    private final IGrid grid;
    private final IGridRegion reg;
    private final MetaInfoReader metaInfoReader;

    private void setStyle(ICell cell, CellModel cm) {
        var style = cell.getStyle();

        if (style == null) {
            return;
        }

        switch (style.getHorizontalAlignment()) {
            case LEFT -> { /* Left by default */ }
            case RIGHT -> cm.setHalign("right");
            case CENTER, CENTER_SELECTION -> cm.setHalign("center");
            case JUSTIFY -> cm.setHalign("justify");
            default -> {
                // Excel aligns a number or a date right, unless the cell is indented: an indent is taken from the left
                if (cell.getNativeType() == IGrid.CELL_TYPE_NUMERIC && style.getIndent() == 0) {
                    cm.setHalign("right");
                }
            }
        }

        switch (style.getVerticalAlignment()) {
            case BOTTOM -> { /* Bottom by default */ }
            case CENTER -> cm.setValign("center");
            case TOP -> cm.setValign("top");
            default -> { /* Other alignments are drawn as bottom */ }
        }

        if (style.getIndent() > 0) {
            cm.setIndent(style.getIndent());
        }

        var rgb = style.getFillForegroundColor();
        cm.setRgbBackground(rgb);

        cm.setFont(cell.getFont());
    }

    CellModel buildCell(ICell cell, CellModel cm) {
        cm.setColspan(getColSpan(cell));
        cm.setRowspan(getRowSpan(cell));
        setStyle(cell, cm);
        return cm;
    }

    TableModel buildModel(IGridTable gt) {
        var tm = new TableModel(GridRegionUtils.width(reg), GridRegionUtils.height(reg), gt);

        if (gt.getGrid() instanceof CompositeGrid compositeGrid) {
            metaInfoReader.prepare(compositeGrid.getGridTables()[0].getRegion());
        } else {
            metaInfoReader.prepare(reg);
        }

        for (var gridRow = reg.getTop(); gridRow <= reg.getBottom(); gridRow++) {
            addDisplayedCellToTableModel(tm, gridRow, gridRow - reg.getTop());
        }

        setGrid(tm);
        return tm;
    }

    private void addDisplayedCellToTableModel(TableModel tm, int gridRow, int displayedRowIndex) {
        for (var column = reg.getLeft(); column <= reg.getRight(); column++) {
            var c = column - reg.getLeft();
            if (tm.hasCell(displayedRowIndex, c)) {
                continue;
            }
            var cm = buildCell(grid.getCell(column, gridRow), new CellModel(displayedRowIndex, c));
            tm.addCell(cm, displayedRowIndex, c);
            if (cm.getColspan() > 1 || cm.getRowspan() > 1) {
                spread(tm, cm, displayedRowIndex, c);
            }
        }
    }

    /** Points every place a merged cell reaches over at the cell the merge belongs to. */
    private static void spread(TableModel tm, CellModel cm, int row, int column) {
        var cmd = new CellModelDelegator(cm);
        for (var i = 0; i < cm.getRowspan(); i++) {
            for (var j = 0; j < cm.getColspan(); j++) {
                if (i > 0 || j > 0) {
                    tm.addCell(cmd, row + i, column + j);
                }
            }
        }
    }

    BorderStyle getBorderStyle(ICellStyle cs, int side) {
        var bss = cs.getBorderStyle();
        var rgbb = cs.getBorderRGB();
        return BorderStyle.of(bss == null ? org.apache.poi.ss.usermodel.BorderStyle.NONE : bss[side],
                rgbb == null ? new short[]{0, 0, 0} : rgbb[side]);
    }

    int getColSpan(ICell cell) {
        var gr = cell.getRegion();
        if (gr == null) {
            return 1;
        }
        IGridRegion intersect = GridRegionUtils.intersect(reg, gr);
        return intersect != null ? GridRegionUtils.width(intersect) : 1;
    }

    int getRowSpan(ICell cell) {
        var gr = cell.getRegion();
        if (gr == null) {
            return 1;
        }
        IGridRegion intersect = GridRegionUtils.intersect(reg, gr);
        return intersect != null ? GridRegionUtils.height(intersect) : 1;
    }

    /**
     * Gives every cell the borders it has itself. A line two cells share may be named by both of them, or by either:
     * the screen draws the wider one, as Excel does. A side of a merged cell takes the border of the cells along that
     * edge, a border drawn on one of them winning over the others drawing none.
     */
    void setGrid(TableModel tm) {
        var left = reg.getLeft();
        var top = reg.getTop();
        for (var row = 0; row < tm.getHeight(); row++) {
            for (var column = 0; column < GridRegionUtils.width(reg); column++) {
                var style = grid.getCell(column + left, row + top).getStyle();
                if (style != null) {
                    for (var side : SIDES) {
                        setBorder(tm.findCellModel(column, row, side), side, getBorderStyle(style, side));
                    }
                }
            }
        }
    }

    /** Sets a side of a cell, unless the side already holds a border the workbook draws and the new one is none. */
    private static void setBorder(@Nullable CellModel model, int side, BorderStyle border) {
        if (model == null) {
            return;
        }
        var sides = model.getBorderStyle();
        var current = sides == null ? null : sides[side];
        if (border != BorderStyle.NONE || current == null) {
            model.setBorderStyle(border, side);
        }
    }

}
