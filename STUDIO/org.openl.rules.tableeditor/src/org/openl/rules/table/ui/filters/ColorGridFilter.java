package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.CellFont;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.IGridSelector;

/**
 * @author snshor
 */
public class ColorGridFilter extends AGridFilter {

    public static final int FONT = 1;
    public static final int BACKGROUND = 2;
    public static final int BORDERS = 4;
    public static final int ALL = 0xFF;

    private int scope = ALL;

    private final IColorFilter filter;

    public ColorGridFilter(IGridSelector selector, IColorFilter filter) {
        super(selector);
        this.filter = filter;
    }

    public ColorGridFilter(IGridSelector selector, IColorFilter filter, int scope) {
        super(selector);
        this.filter = filter;
        this.scope = scope;
    }

    @Override
    public FormattedCell filterFormat(FormattedCell formattedCell) {
        if ((scope & FONT) != 0) {
            var cellFont = (CellFont) formattedCell.getFont();
            var fc = cellFont.getFontColor();
            if (fc == null) {
                fc = IColorFilter.BLACK;
            }
            cellFont.setFontColor(filter.filterColor(fc));
        }

        var cellStyle = (CellStyle) formattedCell.getStyle();
        if ((scope & BACKGROUND) != 0) {
            var bcg = cellStyle.getFillBackgroundColor();
            if (bcg == null) {
                bcg = IColorFilter.WHITE;
            }

            cellStyle.setFillBackgroundColor(filter.filterColor(bcg));

            var fg = cellStyle.getFillForegroundColor();

            if (fg == null) {
                fg = IColorFilter.WHITE;
            }

            cellStyle.setFillForegroundColor(filter.filterColor(fg));
        }

        if ((scope & BORDERS) != 0) {
            var bb = cellStyle.getBorderRGB();

            if (bb != null) {
                for (var i = 0; i < bb.length; i++) {
                    if (bb[i] != null) {
                        bb[i] = filter.filterColor(bb[i]);
                    }
                }
            }
            cellStyle.setBorderRGB(bb);
        }

        return formattedCell;
    }

}
