package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridSelector;

public class CellStyleGridFilter extends AGridFilter {
    private static final int BORDER_SIDES_COUNT = 4;

    private Integer horizontalAlignment;

    private Integer verticalAlignment;

    private short[] fillBackgroundColor;

    private short[] fillForegroundColor;

    private Short fillBackgroundColorIndex;

    private Short fillForegroundColorIndex;

    private Short fillPattern;

    private Short[] borderStyle;

    private short[][] borderRGB;
    private Integer ident;

    private Boolean wrappedText;

    private Integer rotation;

    protected CellStyleGridFilter(IGridSelector selector, Integer horizontalAlignment, Integer verticalAlignment,
            short[] fillBackgroundColor, short[] fillForegroundColor, Short fillBackgroundColorIndex,
            Short fillForegroundColorIndex, Short fillPattern, Short[] borderStyle, short[][] borderRGB, Integer ident,
            Boolean wrappedText, Integer rotation) {
        super(selector);
        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
        this.fillBackgroundColor = fillBackgroundColor;
        this.fillForegroundColor = fillForegroundColor;
        this.fillBackgroundColorIndex = fillBackgroundColorIndex;
        this.fillForegroundColorIndex = fillForegroundColorIndex;
        this.fillPattern = fillPattern;
        this.borderStyle = borderStyle;
        this.borderRGB = borderRGB;
        this.ident = ident;
        this.wrappedText = wrappedText;
        this.rotation = rotation;
    }

    private CellStyleGridFilter(Builder builder) {
        this(builder.selector, builder.horizontalAlignment, builder.verticalAlignment, builder.fillBackgroundColor,
                builder.fillForegroundColor, builder.fillBackgroundColorIndex, builder.fillForegroundColorIndex,
                builder.fillPattern, builder.borderStyle, builder.borderRGB, builder.ident, builder.wrappedText,
                builder.rotation);
    }

    @Override
    public FormattedCell filterFormat(FormattedCell cell) {
        CellStyle style = (CellStyle) cell.getStyle();

        if (horizontalAlignment != null) {
            style.setHorizontalAlignment(horizontalAlignment);
        }
        if (verticalAlignment != null) {
            style.setVerticalAlignment(verticalAlignment);
        }
        if (fillBackgroundColor != null) {
            style.setFillBackgroundColor(fillBackgroundColor);
        }
        if (fillForegroundColor != null) {
            style.setFillForegroundColor(fillForegroundColor);
        }
        if (fillBackgroundColorIndex != null) {
            style.setFillForegroundColorIndex(fillBackgroundColorIndex);
        }
        if (fillForegroundColorIndex != null) {
            style.setFillForegroundColorIndex(fillForegroundColorIndex);
        }
        if (fillPattern != null) {
            style.setFillPattern(fillPattern);
        }
        if (borderStyle != null) {
            if (style.getBorderStyle() != null) {
                for (int i = 0; i < borderStyle.length; i++) {
                    Short border = borderStyle[i];
                    if (border == null) {
                        borderStyle[i] = style.getBorderStyle()[i];
                    }
                }
            }
            style.setBorderStyle(toPrimitive(borderStyle));
        }
        if (borderRGB != null) {
            style.setBorderRGB(borderRGB);
        }
        if (ident != null) {
            style.setIdent(ident);
        }
        if (wrappedText != null) {
            style.setWrappedText(wrappedText);
        }
        if (rotation != null) {
            style.setRotation(rotation);
        }

        return cell;
    }

    public CellStyleGridFilter createUpperRowBorderFilter() {
        IGridSelector upperRowSelector = new IGridSelector() {
            @Override
            public boolean selectCoords(int col, int row) {
                return getGridSelector().selectCoords(col, row + 1) && !getGridSelector().selectCoords(col, row);
            }
        };

        Short[] bottomBorderStyle = new Short[4];
        bottomBorderStyle[ICellStyle.BOTTOM] = ICellStyle.BORDER_NONE;

        short[][] bottomRGB = new short[4][];
        bottomRGB[ICellStyle.BOTTOM] = borderRGB[ICellStyle.TOP];

        return new Builder()
            .setSelector(upperRowSelector)
            .setBorderStyle(bottomBorderStyle)
            .setBorderRGB(bottomRGB)
            .build();
    }

    public CellStyleGridFilter createLefterColumnBorderFilter() {
        IGridSelector upperRowSelector = new IGridSelector() {
            @Override
            public boolean selectCoords(int col, int row) {
                return getGridSelector().selectCoords(col + 1, row) && !getGridSelector().selectCoords(col, row);
            }
        };

        Short[] bottomBorderStyle = new Short[4];
        bottomBorderStyle[ICellStyle.RIGHT] = ICellStyle.BORDER_NONE;

        short[][] bottomRGB = new short[4][];
        bottomRGB[ICellStyle.RIGHT] = borderRGB[ICellStyle.LEFT];

        return new Builder()
            .setSelector(upperRowSelector)
            .setBorderStyle(bottomBorderStyle)
            .setBorderRGB(bottomRGB)
            .build();
    }

    private short[] toPrimitive(Short[] array) {
        short[] primitive = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            primitive[i] = array[i] != null ? array[i] : ICellStyle.BORDER_NONE;
        }
        return primitive;
    }

    public static class Builder {
        private IGridSelector selector;

        private Integer horizontalAlignment;

        private Integer verticalAlignment;

        private short[] fillBackgroundColor;

        private short[] fillForegroundColor;

        private Short fillBackgroundColorIndex;

        private Short fillForegroundColorIndex;

        private Short fillPattern;

        private Short[] borderStyle;

        private short[][] borderRGB;
        private Integer ident;

        private Boolean wrappedText;

        private Integer rotation;

        public Builder setSelector(IGridSelector selector) {
            if (selector == null) {
                throw new IllegalArgumentException("selector can't be null");
            }

            this.selector = selector;
            return this;
        }

        public Builder setHorizontalAlignment(int horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }

        public Builder setVerticalAlignment(int verticalAlignment) {
            this.verticalAlignment = verticalAlignment;
            return this;
        }

        public Builder setFillBackgroundColor(short[] fillBackgroundColor) {
            if (fillBackgroundColor == null) {
                throw new IllegalArgumentException("fillBackgroundColor can't be null");
            }

            this.fillBackgroundColor = fillBackgroundColor;
            return this;
        }

        public Builder setFillForegroundColor(short[] fillForegroundColor) {
            if (fillForegroundColor == null) {
                throw new IllegalArgumentException("fillForegroundColor can't be null");
            }

            this.fillForegroundColor = fillForegroundColor;
            return this;
        }

        public Builder setFillBackgroundColorIndex(short fillBackgroundColorIndex) {
            this.fillBackgroundColorIndex = fillBackgroundColorIndex;
            return this;
        }

        public Builder setFillForegroundColorIndex(short fillForegroundColorIndex) {
            this.fillForegroundColorIndex = fillForegroundColorIndex;
            return this;
        }

        public Builder setFillPattern(short fillPattern) {
            this.fillPattern = fillPattern;
            return this;
        }

        public Builder setBorderStyle(Short[] borderStyle) {
            if (borderStyle == null) {
                throw new IllegalArgumentException("borderStyle can't be null");
            }

            this.borderStyle = borderStyle;
            return this;
        }

        public Builder setBorderStyle(short borderStyles) {
            return setBorderStyle(createBorderStyle(borderStyles));
        }

        public Builder setBorderRGB(short[][] borderRGB) {
            if (borderRGB == null) {
                throw new IllegalArgumentException("borderRGB can't be null");
            }

            this.borderRGB = borderRGB;
            return this;
        }

        public Builder setBorderRGB(short[] rgb) {
            if (rgb == null) {
                throw new IllegalArgumentException("rgb can't be null");
            }

            return setBorderRGB(createBorderRGB(rgb));
        }

        public Builder setIdent(int ident) {
            this.ident = ident;
            return this;
        }

        public Builder setWrappedText(boolean wrappedText) {
            this.wrappedText = wrappedText;
            return this;
        }

        public Builder setRotation(int rotation) {
            this.rotation = rotation;
            return this;
        }

        public CellStyleGridFilter build() {
            return new CellStyleGridFilter(this);
        }

        private Short[] createBorderStyle(short style) {
            Short[] colors = new Short[BORDER_SIDES_COUNT];
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
