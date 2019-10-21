package org.openl.rules.table.ui.filters;

import java.util.Objects;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.CellFont;
import org.openl.rules.table.ui.IGridSelector;

/**
 * A grid filter that changes a font's properties
 *
 * @author NSamatov
 */
public class FontGridFilter extends AGridFilter {
    private short[] fontColor;

    private Integer size;
    private Integer incrementSize;

    private String name;

    private Boolean italic;

    private Boolean bold;

    private Boolean underlined;

    private Boolean strikeout;

    /**
     * Initialize a filter with some parameters. If any parameter is null, it is not changed when filtering. Instead
     * this constructor usage of a builder is preferred.
     *
     * @param selector
     * @param fontColor
     * @param size
     * @param incrementSize
     * @param name
     * @param italic
     * @param bold
     * @param underlined
     * @param strikeout
     * @see Builder
     */
    protected FontGridFilter(IGridSelector selector,
            short[] fontColor,
            Integer size,
            Integer incrementSize,
            String name,
            Boolean italic,
            Boolean bold,
            Boolean underlined,
            Boolean strikeout) {
        super(selector);

        this.fontColor = fontColor;
        this.size = size;
        this.incrementSize = incrementSize;
        this.name = name;
        this.italic = italic;
        this.bold = bold;
        this.underlined = underlined;
        this.strikeout = strikeout;
    }

    private FontGridFilter(Builder builder) {
        this(builder.selector,
            builder.fontColor,
            builder.size,
            builder.incrementSize,
            builder.name,
            builder.italic,
            builder.bold,
            builder.underlined,
            builder.strikeout);
    }

    @Override
    public FormattedCell filterFormat(FormattedCell cell) {
        CellFont font = (CellFont) cell.getFont();

        if (fontColor != null) {
            font.setFontColor(fontColor);
        }

        if (size != null) {
            font.setSize(size);
        }

        if (incrementSize != null) {
            font.setSize(font.getSize() + incrementSize);
        }

        if (name != null) {
            font.setName(name);
        }

        if (italic != null) {
            font.setItalic(italic);
        }

        if (bold != null) {
            font.setBold(bold);
        }

        if (underlined != null) {
            font.setUnderlined(underlined);
        }

        if (strikeout != null) {
            font.setStrikeout(strikeout);
        }

        return cell;
    }

    /**
     * A builder that creates a FontGridFilter object. If any parameter is omitted, it is not used in filtering grid
     * format.
     *
     * @author NSamatov
     */
    public static class Builder {
        private IGridSelector selector;

        private short[] fontColor;

        private Integer size;
        private Integer incrementSize;

        private String name;

        private Boolean italic;

        private Boolean bold;

        private Boolean underlined;

        private Boolean strikeout;

        public Builder setSelector(IGridSelector selector) {
            this.selector = Objects.requireNonNull(selector, "selector can't be null.");
            return this;
        }

        public Builder setFontColor(short[] fontColor) {
            this.fontColor = Objects.requireNonNull(fontColor, "fontColor can't be null.");
            return this;
        }

        /**
         * Set font size. Do not use in conjunction with {@link #setIncrementSize(int)}
         *
         * @param size new size of a font
         * @return this builder
         */
        public Builder setSize(int size) {
            this.size = size;
            return this;
        }

        /**
         * Increment a font's size to a given value. Do not use in conjunction with {@link #setSize(int)}
         *
         * @param incrementSize increment size
         * @return this builder
         */
        public Builder setIncrementSize(int incrementSize) {
            this.incrementSize = incrementSize;
            return this;
        }

        public Builder setName(String name) {
            this.name = Objects.requireNonNull(name, "name can't be null.");
            return this;
        }

        public Builder setItalic(boolean italic) {
            this.italic = italic;
            return this;
        }

        public Builder setBold(boolean bold) {
            this.bold = bold;
            return this;
        }

        public Builder setUnderlined(boolean underlined) {
            this.underlined = underlined;
            return this;
        }

        public Builder setStrikeout(boolean strikeout) {
            this.strikeout = strikeout;
            return this;
        }

        public FontGridFilter build() {
            if (size != null && incrementSize != null) {
                throw new IllegalArgumentException(
                    "Only one of 'size' and 'incrementSize' paremeters should be initialized");
            }

            return new FontGridFilter(this);
        }
    }

}
