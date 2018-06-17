package org.openl.excel.grid;

import org.apache.poi.ss.usermodel.*;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.table.ui.ICellStyle;

class IndentedStyle implements ICellStyle {
    private final short indent;

    private final ParsedGrid parsedGrid;
    private final int row;
    private final int column;

    private transient ICellStyle delegate;

    IndentedStyle(short indent, ParsedGrid parsedGrid, int row, int column) {
        this.indent = indent;
        this.parsedGrid = parsedGrid;
        this.row = row;
        this.column = column;
    }

    @Override
    public short[][] getBorderRGB() {
        ICellStyle style = getDelegate();
        return style == null ? null : style.getBorderRGB();
    }

    @Override
    public BorderStyle[] getBorderStyle() {
        ICellStyle style = getDelegate();
        return style == null ? null : style.getBorderStyle();
    }

    @Override
    public short[] getFillBackgroundColor() {
        ICellStyle style = getDelegate();
        return style == null ? null : style.getFillBackgroundColor();
    }

    @Override
    public short[] getFillForegroundColor() {
        ICellStyle style = getDelegate();
        return style == null ? null : style.getFillForegroundColor();
    }

    @Override
    public short getFillBackgroundColorIndex() {
        ICellStyle style = getDelegate();
        return style == null ? IndexedColors.AUTOMATIC.getIndex() : style.getFillBackgroundColorIndex();
    }

    @Override
    public short getFillForegroundColorIndex() {
        ICellStyle style = getDelegate();
        return style == null ? IndexedColors.AUTOMATIC.getIndex() : style.getFillForegroundColorIndex();
    }

    @Override
    public FillPatternType getFillPattern() {
        ICellStyle style = getDelegate();
        return style == null ? FillPatternType.NO_FILL : style.getFillPattern();
    }

    @Override
    public HorizontalAlignment getHorizontalAlignment() {
        ICellStyle style = getDelegate();
        return style == null ? HorizontalAlignment.LEFT : style.getHorizontalAlignment();
    }

    /**
     * Needed during project compilation. Should be pre-loaded.
     */
    @Override
    public int getIndent() {
        return indent;
    }

    @Override
    public int getRotation() {
        ICellStyle style = getDelegate();
        return style == null ? 0 : style.getRotation();
    }

    @Override
    public VerticalAlignment getVerticalAlignment() {
        ICellStyle style = getDelegate();
        return style == null ? VerticalAlignment.TOP : style.getVerticalAlignment();
    }

    @Override
    public boolean isWrappedText() {
        ICellStyle style = getDelegate();
        return style != null && style.isWrappedText();
    }

    @Override
    public short getFormatIndex() {
        ICellStyle style = getDelegate();
        return style == null ? 0 : style.getFormatIndex();
    }

    @Override
    public String getFormatString() {
        ICellStyle style = getDelegate();
        return style == null ? "" : style.getFormatString();
    }

    private ICellStyle getDelegate() {
        if (delegate == null) {
            // Lazy load
            TableStyles tableStyles = parsedGrid.getTableStyles(row, column);
            delegate = tableStyles == null ? null : tableStyles.getStyle(row, column);
        }
        return delegate;
    }
}
