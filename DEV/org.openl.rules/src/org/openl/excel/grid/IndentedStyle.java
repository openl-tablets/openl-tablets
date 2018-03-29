package org.openl.excel.grid;

import org.apache.poi.ss.usermodel.*;
import org.openl.rules.table.ui.ICellStyle;

class IndentedStyle implements ICellStyle {
    private final short indent;

    IndentedStyle(short indent) {
        this.indent = indent;
    }

    @Override
    public short[][] getBorderRGB() {
        return null;
    }

    @Override
    public BorderStyle[] getBorderStyle() {
        return null;
    }

    @Override
    public short[] getFillBackgroundColor() {
        return null;
    }

    @Override
    public short[] getFillForegroundColor() {
        return null;
    }

    @Override
    public short getFillBackgroundColorIndex() {
        return IndexedColors.AUTOMATIC.getIndex();
    }

    @Override
    public short getFillForegroundColorIndex() {
        return IndexedColors.AUTOMATIC.getIndex();
    }

    @Override
    public FillPatternType getFillPattern() {
        return FillPatternType.NO_FILL;
    }

    @Override
    public HorizontalAlignment getHorizontalAlignment() {
        return HorizontalAlignment.LEFT;
    }

    @Override
    public int getIndent() {
        return indent;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public VerticalAlignment getVerticalAlignment() {
        return VerticalAlignment.TOP;
    }

    @Override
    public boolean isWrappedText() {
        return false;
    }
}
