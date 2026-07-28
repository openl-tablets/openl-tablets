package org.openl.rules.tableeditor.model.ui;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.StringUtils;

@RequiredArgsConstructor
@Slf4j
public class TableViewer {

    private final IGrid grid;
    private final IGridRegion reg;
    private final LinkBuilder linkBuilder;
    private final String mode;
    private final String view;
    private final MetaInfoReader metaInfoReader;
    private final boolean smartNumbers;

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

    CellModel buildCell(ICell cell, CellModel cm, CellMetaInfo metaInfo) {
        cm.setColspan(getColSpan(cell));
        cm.setRowspan(getRowSpan(cell));

        if (cm.getRow() == 0) {
            cm.setWidth(getWidth(cell));
        }

        String formattedValue = XlsDataFormatterFactory.getFormattedValue(cell, metaInfo, smartNumbers);
        if (StringUtils.isNotBlank(formattedValue)) {
            String content;
            if (Constants.MODE_EDIT.equals(mode)) {
                // In edit mode there should be no links: it's difficult to start cell editing.
                if (CellMetaInfo.isCellContainsNodeUsages(metaInfo)) {
                    content = createCellWithMetaInfo(formattedValue, metaInfo, false);
                } else {
                    content = escapeHtml4(formattedValue);
                }
            } else if (link(formattedValue)) {
                // has Explanation link
                content = formattedValue;
            } else if (isShowLinks() && (CellMetaInfo
                    .isCellContainsNodeUsages(metaInfo) || (metaInfo != null && metaInfo.isReturnCell()))) {
                // has method call
                content = createCellWithMetaInfo(formattedValue, metaInfo, true);
            } else if (image(formattedValue)) {
                // has image
                content = formattedValue;
            } else if (error(formattedValue)) {
                content = formattedValue;
            } else {
                content = escapeHtml4(formattedValue);
            }
            cm.setContent(content);
            if (cell.getFormula() != null) {
                cm.setFormula(cell.getFormula());
            }
        }

        var cellComment = cell.getComment();
        cm.setComment(cellComment != null ? cellComment.getText() : null);

        setStyle(cell, cm);
        return cm;
    }

    private boolean image(String formattedValue) {
        return formattedValue.replaceAll("\n", "").matches(".*<i .*>.*</i>.*");
    }

    private boolean error(String formattedValue) {
        return formattedValue.matches(".*<span style=\"color: red;\".*>.*</span>.*");
    }

    private boolean link(String formattedValue) {
        return formattedValue.matches(".*<a href.*</a>.*");
    }

    private String createCellWithMetaInfo(String formattedValue, CellMetaInfo metaInfo, boolean addUri) {
        try {
            var nextSymbolIndex = 0;
            var buff = new StringBuilder();
            if (metaInfo.getUsedNodes() != null) {
                for (NodeUsage nodeUsage : metaInfo.getUsedNodes()) {
                    var pstart = nodeUsage.getStart();
                    var pend = nodeUsage.getEnd();
                    var tableUri = nodeUsage.getUri();
                    buff.append(escapeHtml4(formattedValue.substring(nextSymbolIndex, pstart)));
                    // add link to used table with signature in tooltip
                    buff.append("<span class=\"title")
                            .append(" title-")
                            .append(nodeUsage.getNodeType().toString().toLowerCase(Locale.ROOT))
                            .append(" ")
                            .append(Constants.TABLE_EDITOR_META_INFO_CLASS)
                            .append("\">");
                    if (addUri && tableUri != null) {
                        buff.append(
                                linkBuilder.createLinkForTable(tableUri, formattedValue.substring(pstart, pend)));
                    } else {
                        buff.append(escapeHtml4(formattedValue.substring(pstart, pend)));
                    }
                    buff.append("<em>").append(escapeHtml4(nodeUsage.getDescription())).append("</em></span>");
                    nextSymbolIndex = pend;
                }
            }
            buff.append(escapeHtml4(formattedValue.substring(nextSymbolIndex)));

            if (metaInfo.isReturnCell()) {
                buff.append("<span class=\"title title-")
                        .append(NodeType.OTHER.toString().toLowerCase(Locale.ROOT))
                        .append(" ")
                        .append(Constants.TABLE_EDITOR_META_INFO_CLASS)
                        .append("\">");
                buff.append("  &#9733;");
                buff.append("<em>RETURN</em></span>");
            }

            return buff.toString();
        } catch (RuntimeException e) {
            // Fallback to the formula without links
            log.error(e.getMessage(), e);
            return escapeHtml4(formattedValue);
        }
    }

    private boolean isShowLinks() {
        return linkBuilder != null;
    }

    public TableModel buildModel(IGridTable gt, int numRowsToDisplay, List<ICell> modifiedCells, IGridRegion region) {
        var h = IGridRegion.Tool.height(region);
        var w = IGridRegion.Tool.width(region);

        var showHeader = true;
        if ("business".equals(view)) {
            showHeader = false;
        }

        var tm = new TableModel(w, h, gt, showHeader);
        tm.setNumRowsToDisplay(numRowsToDisplay);

        if (gt.getGrid() instanceof CompositeGrid) {
            metaInfoReader.prepare(((CompositeGrid) gt.getGrid()).getGridTables()[0].getRegion());
        } else {
            metaInfoReader.prepare(reg);
        }

        if (modifiedCells != null) {
            var modifiedRows = modifiedCells.stream().map(ICell::getRow).collect(Collectors.toSet());
            var lastModifiedRow = modifiedRows.stream().max(Integer::compareTo).orElse(0);
            if (lastModifiedRow >= h) {
                tm = new TableModel(w, lastModifiedRow + 1, gt, showHeader);
            }
            if (numRowsToDisplay > -1) {
                if (numRowsToDisplay < modifiedRows.size()) {
                    modifiedRows = modifiedRows.stream().limit(numRowsToDisplay).collect(Collectors.toSet());
                } else {
                    tm.setNumRowsToDisplay(-1);
                }
            }
            for (int row : modifiedRows) {
                var gridRow = row + region.getTop();
                var count = modifiedCells.stream().filter(c -> c.getRow() == row).count();
                addDisplayedCellToTableModel(tm, gridRow, row, region, count == w ? modifiedCells : null);
            }
        } else {
            for (var gridRow = region.getTop(); gridRow <= region.getBottom(); gridRow++) {
                var row = gridRow - region.getTop();
                addDisplayedCellToTableModel(tm, gridRow, row, region, null);
            }
        }

        setGrid(tm);
        return tm;
    }

    private void addDisplayedCellToTableModel(TableModel tm,
                                              int gridRow,
                                              int displayedRowIndex,
                                              IGridRegion region,
                                              List<ICell> modifiedCells) {
        for (var column = region.getLeft(); column <= region.getRight(); column++) {
            var c = column - region.getLeft();
            if (tm.hasCell(displayedRowIndex, c)) {
                continue;
            }
            Optional<ICell> changedCell = Optional.empty();
            if (modifiedCells != null) {
                changedCell = modifiedCells.stream()
                        .filter(v -> v.getRow() == displayedRowIndex && v.getColumn() == c)
                        .findFirst();
            }
            var cell = changedCell.orElse(grid.getCell(column, gridRow));
            var metaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
            var cm = buildCell(cell, new CellModel(displayedRowIndex, c), metaInfo);
            tm.addCell(cm, displayedRowIndex, c);
            if (cm.getColspan() > 1 || cm.getRowspan() > 1) {
                var cmd = new CellModelDelegator(cm);
                for (var i = 0; i < cm.getRowspan(); i++) {
                    for (var j = 0; j < cm.getColspan(); j++) {
                        if (i == 0 && j == 0) {
                            continue;
                        }
                        tm.addCell(cmd, displayedRowIndex + i, c + j);
                    }
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

    public int getWidth(ICell cell) {
        IGridRegion gr;
        if ((gr = cell.getRegion()) == null) {
            return grid.getColumnWidth(cell.getColumn());
        }
        var w = 0;

        gr = IGridRegion.Tool.intersect(gr, reg);
        if (gr != null) {
            for (var c = gr.getLeft(); c <= gr.getRight(); c++) {
                w += grid.getColumnWidth(c);
            }
        }

        return w;
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
