package org.openl.rules.tableeditor.model.ui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;

/** Lays a table's region out place by place, so that every place of the grid says what stands there. */
@RequiredArgsConstructor
@Slf4j
public class TableViewer {

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
            case CENTER -> cm.setHalign("center");
            case JUSTIFY -> cm.setHalign("justify");
            default -> {
                // Align right numeric and date
                if (cell.getNativeType() == IGrid.CELL_TYPE_NUMERIC) {
                    cm.setHalign("right");
                }
            }
        }

        switch (style.getVerticalAlignment()) {
            case BOTTOM -> { /* Bottom by default */ }
            case CENTER -> cm.setValign("center");
            case TOP -> cm.setValign("top");
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

    public TableModel buildModel(IGridTable gt) {
        var tm = new TableModel(IGridRegion.Tool.width(reg), IGridRegion.Tool.height(reg), gt);

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

        org.apache.poi.ss.usermodel.BorderStyle xlsStyle;
        short[] rgb;

        var bss = cs.getBorderStyle();
        xlsStyle = bss == null ? org.apache.poi.ss.usermodel.BorderStyle.NONE : bss[side];

        var rgbb = cs.getBorderRGB();
        rgb = rgbb == null ? new short[]{0, 0, 0} : rgbb[side];

        var bs = new BorderStyle();
        bs.setRgb(rgb);
        switch (xlsStyle) {
            case NONE -> {
                return BorderStyle.NONE;
            }
            case DASH_DOT_DOT, DASH_DOT, DASHED -> {
                bs.setWidth(1);
                bs.setStyle("dashed");
            }
            case DOTTED -> {
                bs.setWidth(1);
                bs.setStyle("dotted");
            }
            case DOUBLE -> {
                bs.setWidth(1);
                bs.setStyle("double");
            }
            case THIN -> {
                bs.setWidth(1);
                bs.setStyle("solid");
            }
            case THICK -> {
                bs.setWidth(2);
                bs.setStyle("solid");
            }
            case HAIR -> {
                bs.setWidth(1);
                bs.setStyle("dotted");
            }
            case MEDIUM -> {
                bs.setWidth(2);
                bs.setStyle("solid");
            }
            case MEDIUM_DASH_DOT, MEDIUM_DASH_DOT_DOT, MEDIUM_DASHED -> {
                bs.setWidth(2);
                bs.setStyle("dashed");
            }
            default -> {
                log.warn("Unknown border style: {}", xlsStyle);
                bs.setWidth(1);
                bs.setStyle("solid");
            }
        }
        return bs;
    }

    int getColSpan(ICell cell) {
        var gr = cell.getRegion();
        if (gr == null) {
            return 1;
        }
        IGridRegion intersect = IGridRegion.Tool.intersect(reg, gr);
        return intersect != null ? IGridRegion.Tool.width(intersect) : 1;
    }

    int getRowSpan(ICell cell) {
        var gr = cell.getRegion();
        if (gr == null) {
            return 1;
        }
        IGridRegion intersect = IGridRegion.Tool.intersect(reg, gr);
        return intersect != null ? IGridRegion.Tool.height(intersect) : 1;
    }

    short[] rgb(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null && bs2 == null) {
            return new short[]{0, 0, 0};
        }

        return bs1 == null ? bs2.getRgb()
                : bs2 == null ? bs1.getRgb() : bs1 == BorderStyle.NONE ? bs2.getRgb() : bs1.getRgb();
    }

    void setGrid(TableModel tm) {
        var width = IGridRegion.Tool.width(reg);

        for (var i = 0; i <= width; i++) {
            setVerticalBorder(i, tm);
        }

        var height = tm.getHeight();

        for (var i = 0; i <= height; i++) {
            setHorizontalBorder(i, tm);
        }

    }

    void setHorizontalBorder(int row, TableModel tm) {
        var width = IGridRegion.Tool.width(reg);
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

            var W = width(tStyle, bStyle);
            var style = style(tStyle, bStyle);
            var rgb = rgb(tStyle, bStyle);

            var bstyle = new BorderStyle(W, style, rgb);

            switch (W) {
                case 0 -> { /* No border */ }
                case 1 -> {
                    if (cmTop == null) {
                        cmBottom.setBorderStyle(bstyle, ICellStyle.TOP);
                    } else {
                        cmTop.setBorderStyle(bstyle, ICellStyle.BOTTOM);
                    }
                }
                case 2 -> {
                    if (cmTop == null) {
                        cmBottom.setBorderStyle(bstyle, ICellStyle.TOP);
                    } else if (cmBottom == null) {
                        cmTop.setBorderStyle(bstyle, ICellStyle.BOTTOM);
                    } else {
                        bstyle.setWidth(1);
                        cmBottom.setBorderStyle(bstyle, ICellStyle.TOP);
                        cmTop.setBorderStyle(bstyle, ICellStyle.BOTTOM);
                    }
                }
                default -> { }
            }
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

            var W = width(lStyle, rStyle);
            var style = style(lStyle, rStyle);
            var rgb = rgb(lStyle, rStyle);

            var bstyle = new BorderStyle(W, style, rgb);

            switch (W) {
                case 0 -> { /* No border */ }
                case 1 -> {
                    if (cmLeft == null) {
                        cmRight.setBorderStyle(bstyle, ICellStyle.LEFT);
                    } else {
                        cmLeft.setBorderStyle(bstyle, ICellStyle.RIGHT);
                    }
                }
                case 2 -> {
                    if (cmLeft == null) {
                        cmRight.setBorderStyle(bstyle, ICellStyle.LEFT);
                    } else if (cmRight == null) {
                        cmLeft.setBorderStyle(bstyle, ICellStyle.RIGHT);
                    } else {
                        bstyle.setWidth(1);
                        cmRight.setBorderStyle(bstyle, ICellStyle.LEFT);
                        cmLeft.setBorderStyle(bstyle, ICellStyle.RIGHT);
                    }
                }
                default -> { }
            }
        }

    }

    String style(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null && bs2 == null) {
            return "none";
        }

        return bs1 == null ? bs2.getStyle()
                : bs2 == null ? bs1.getStyle() : bs1 == BorderStyle.NONE ? bs2.getStyle() : bs1.getStyle();
    }

    int width(BorderStyle bs1, BorderStyle bs2) {
        if (bs1 == null && bs2 == null) {
            return 0;
        }

        return bs1 == null ? bs2.getWidth() : bs2 == null ? bs1.getWidth() : Math.max(bs1.getWidth(), bs2.getWidth());
    }

}
