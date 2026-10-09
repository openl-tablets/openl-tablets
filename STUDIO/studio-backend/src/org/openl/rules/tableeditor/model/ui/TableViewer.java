package org.openl.rules.tableeditor.model.ui;

import lombok.RequiredArgsConstructor;

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

    void setGrid(TableModel tm) {
        var width = GridRegionUtils.width(reg);

        for (var i = 0; i <= width; i++) {
            setVerticalBorder(i, tm);
        }

        var height = tm.getHeight();

        for (var i = 0; i <= height; i++) {
            setHorizontalBorder(i, tm);
        }

    }

    void setHorizontalBorder(int row, TableModel tm) {
        var width = GridRegionUtils.width(reg);
        var left = reg.getLeft();
        var top = reg.getTop();

        for (var i = 0; i < width; i++) {
            ICellStyle ts = row + top - 1 < 0 ? null : grid.getCell(i + left, row + top - 1).getStyle();
            var bs = grid.getCell(i + left, row + top).getStyle();

            CellModel cmTop = ts == null ? null : tm.findCellModel(i, row - 1, ICellStyle.BOTTOM);
            CellModel cmBottom = bs == null ? null : tm.findCellModel(i, row, ICellStyle.TOP);

            if (cmTop == null && cmBottom == null) {
                continue;
            }

            BorderStyle tStyle = ts != null ? getBorderStyle(ts, ICellStyle.BOTTOM) : null;
            BorderStyle bStyle = bs != null ? getBorderStyle(bs, ICellStyle.TOP) : null;

            var borderWidth = width(tStyle, bStyle);
            var bstyle = shared(tStyle, bStyle, borderWidth);

            setBorder(borderWidth, bstyle, cmTop, ICellStyle.BOTTOM, cmBottom, ICellStyle.TOP);
        }

    }

    void setVerticalBorder(int column, TableModel tm) {
        var height = tm.getHeight();
        var left = reg.getLeft();
        var top = reg.getTop();

        for (var i = 0; i < height; i++) {
            ICellStyle ls = column + left - 1 < 0 ? null : grid.getCell(column + left - 1, i + top).getStyle();
            var rs = grid.getCell(column + left, i + top).getStyle();

            CellModel cmLeft = ls == null ? null : tm.findCellModel(column - 1, i, ICellStyle.RIGHT);
            CellModel cmRight = rs == null ? null : tm.findCellModel(column, i, ICellStyle.LEFT);

            if (cmLeft == null && cmRight == null) {
                continue;
            }

            BorderStyle lStyle = ls != null ? getBorderStyle(ls, ICellStyle.RIGHT) : null;
            BorderStyle rStyle = rs != null ? getBorderStyle(rs, ICellStyle.LEFT) : null;

            var borderWidth = width(lStyle, rStyle);
            var bstyle = shared(lStyle, rStyle, borderWidth);

            setBorder(borderWidth, bstyle, cmLeft, ICellStyle.RIGHT, cmRight, ICellStyle.LEFT);
        }

    }

    /**
     * Sets the border between two adjacent cells: the first one is above or on the left, the second one is below or
     * on the right, and at least one of them is present. Each side is the side of its cell facing the other cell.
     * <p>
     * A single border goes to the first cell if it is present, otherwise to the second one. A double border goes to
     * the only present cell; when both cells are present, each of them gets a single border.
     */
    private static void setBorder(int borderWidth,
                                  BorderStyle bstyle,
                                  CellModel first,
                                  int firstSide,
                                  CellModel second,
                                  int secondSide) {
        switch (borderWidth) {
            case 0 -> { /* No border */ }
            case 1 -> {
                if (first == null) {
                    second.setBorderStyle(bstyle, secondSide);
                } else {
                    first.setBorderStyle(bstyle, firstSide);
                }
            }
            case 2 -> {
                if (first == null) {
                    second.setBorderStyle(bstyle, secondSide);
                } else if (second == null) {
                    first.setBorderStyle(bstyle, firstSide);
                } else {
                    var half = new BorderStyle(1, bstyle.getStyle(), bstyle.getRgb());
                    second.setBorderStyle(half, secondSide);
                    first.setBorderStyle(half, firstSide);
                }
            }
            default -> { /* getBorderStyle gives no border wider than 2 */ }
        }
    }

    /**
     * The border two cells share, at least one of them present. Where neither cell has a border, it is
     * {@link BorderStyle#NONE} itself, so a reader of the cell can tell the line of the grid from a border the
     * workbook draws.
     */
    private static BorderStyle shared(BorderStyle bs1, BorderStyle bs2, int borderWidth) {
        var drawn = dominant(bs1, bs2);
        return drawn == BorderStyle.NONE ? BorderStyle.NONE
                : new BorderStyle(borderWidth, drawn.getStyle(), drawn.getRgb());
    }

    int width(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null && bs2 == null) {
            return 0;
        }

        if (bs1 == null) {
            return bs2.getWidth();
        }
        return bs2 == null ? bs1.getWidth() : Math.max(bs1.getWidth(), bs2.getWidth());
    }

    /**
     * Picks the style drawn on a border two cells share: the first one, unless it is missing or it is
     * {@link BorderStyle#NONE} while the second one is present.
     */
    private static BorderStyle dominant(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null) {
            return bs2;
        }
        if (bs2 == null) {
            return bs1;
        }
        return bs1 == BorderStyle.NONE ? bs2 : bs1;
    }

}
