package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.CellFont;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.IGridSelector;

/**
 * @author snshor
 *
 */
public class ColorGridFilter extends AGridFilter {

    public static final  int FONT = 1, BACKGROUND = 2, BORDERS = 4, ALL = 0xFF;

    private int scope = ALL;

    private IColorFilter filter;

    public ColorGridFilter(IGridSelector selector, IColorFilter filter) {
        super(selector);
        this.filter = filter;
    }

    public ColorGridFilter(IGridSelector selector, IColorFilter filter, int scope) {
        super(selector);
        this.filter = filter;
        this.scope = scope;
    }

    public FormattedCell filterFormat(FormattedCell formattedCell) {
        if ((scope & FONT) != 0) {
            CellFont cellFont = (CellFont) formattedCell.getFont();
            short[] fc = cellFont.getFontColor();
            if (fc == null) {
                fc = IColorFilter.BLACK;
            }
            cellFont.setFontColor(filter.filterColor(fc));
        }

        CellStyle cellStyle = (CellStyle) formattedCell.getStyle();
        if ((scope & BACKGROUND) != 0) {
            short[] bcg = cellStyle.getFillBackgroundColor();
            if (bcg == null) {
                bcg = IColorFilter.WHITE;
            }

            cellStyle.setFillBackgroundColor(filter.filterColor(bcg));

            short[] fg = cellStyle.getFillForegroundColor();

            if (fg == null) {
                fg = IColorFilter.WHITE;
            }

            cellStyle.setFillForegroundColor(filter.filterColor(fg));
        }

        if ((scope & BORDERS) != 0) {
            short[][] bb = cellStyle.getBorderRGB();

            if (bb != null) {
                for (int i = 0; i < bb.length; i++) {
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
