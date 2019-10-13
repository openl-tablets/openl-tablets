package org.openl.rules.table.ui.filters;

import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridSelector;

public class CellStyleGridFilter extends AGridFilter {
    private static final int BORDER_SIDES_COUNT = 4;

    private BorderStyle[] borderStyle;

    private short[][] borderRGB;

    protected CellStyleGridFilter(IGridSelector selector, BorderStyle[] borderStyle, short[][] borderRGB) {
        super(selector);
        this.borderStyle = borderStyle;
        this.borderRGB = borderRGB;
    }

    private CellStyleGridFilter(Builder builder) {
        this(builder.selector, builder.borderStyle, builder.borderRGB);
    }

    @Override
    public FormattedCell filterFormat(FormattedCell cell) {
        CellStyle style = (CellStyle) cell.getStyle();

        if (borderStyle != null) {
            if (style.getBorderStyle() != null) {
                for (int i = 0; i < borderStyle.length; i++) {
                    BorderStyle border = borderStyle[i];
                    if (border == null) {
                        borderStyle[i] = style.getBorderStyle()[i];
                    }
                }
            }
            style.setBorderStyle(borderStyle);
        }
        if (borderRGB != null) {
            style.setBorderRGB(borderRGB);
        }

        return cell;
    }

    public CellStyleGridFilter createUpperRowBorderFilter() {
        IGridSelector upperRowSelector = (col,
                row) -> getGridSelector().selectCoords(col, row + 1) && !getGridSelector().selectCoords(col, row);

        BorderStyle[] bottomBorderStyle = new BorderStyle[4];
        bottomBorderStyle[ICellStyle.BOTTOM] = BorderStyle.NONE;

        short[][] bottomRGB = new short[4][];
        bottomRGB[ICellStyle.BOTTOM] = borderRGB[ICellStyle.TOP];

        return new Builder().setSelector(upperRowSelector)
            .setBorderStyle(bottomBorderStyle)
            .setBorderRGB(bottomRGB)
            .build();
    }

    public CellStyleGridFilter createLefterColumnBorderFilter() {
        IGridSelector upperRowSelector = (col,
                row) -> getGridSelector().selectCoords(col + 1, row) && !getGridSelector().selectCoords(col, row);

        BorderStyle[] bottomBorderStyle = new BorderStyle[4];
        bottomBorderStyle[ICellStyle.RIGHT] = BorderStyle.NONE;

        short[][] bottomRGB = new short[4][];
        bottomRGB[ICellStyle.RIGHT] = borderRGB[ICellStyle.LEFT];

        return new Builder().setSelector(upperRowSelector)
            .setBorderStyle(bottomBorderStyle)
            .setBorderRGB(bottomRGB)
            .build();
    }

    public static class Builder {
        private IGridSelector selector;

        private BorderStyle[] borderStyle;

        private short[][] borderRGB;

        public Builder setSelector(IGridSelector selector) {
            this.selector = Objects.requireNonNull(selector, "selector can't be null.");
            return this;
        }

        public Builder setBorderStyle(BorderStyle[] borderStyle) {
            this.borderStyle = Objects.requireNonNull(borderStyle, "borderStyle can't be null.");
            return this;
        }

        public Builder setBorderStyle(BorderStyle borderStyles) {
            return setBorderStyle(createBorderStyle(borderStyles));
        }

        public Builder setBorderRGB(short[][] borderRGB) {
            this.borderRGB = Objects.requireNonNull(borderRGB, "borderRGB can't be null.");
            return this;
        }

        public Builder setBorderRGB(short[] rgb) {
            Objects.requireNonNull(rgb, "rgb can't be null.");
            return setBorderRGB(createBorderRGB(rgb));
        }

        public CellStyleGridFilter build() {
            return new CellStyleGridFilter(this);
        }

        private BorderStyle[] createBorderStyle(BorderStyle style) {
            BorderStyle[] colors = new BorderStyle[BORDER_SIDES_COUNT];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = style;
            }
            return colors;
        }

        private short[][] createBorderRGB(short[] rgb) {
            short[][] colors = new short[BORDER_SIDES_COUNT][];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = rgb;
            }
            return colors;
        }
    }
}
