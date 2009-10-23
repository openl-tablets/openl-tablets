/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

import org.openl.rules.table.FormattedCell;

/**
 * @author snshor
 *
 */
public class ColorGridFilter extends AGridFilter {

    static final public int FONT = 1, BACKGROUND = 2, BORDERS = 4, ALL = 0xFF;

    int scope = ALL;

    IColorFilter filter;

    static public ColorGridFilter makeTransparentFilter(IGridSelector selector, double transparency, int color) {
        TransparentColorFilter tf = new TransparentColorFilter(color, transparency);

        return new ColorGridFilter(selector, tf);
    }

    public ColorGridFilter(IGridSelector selector, IColorFilter filter) {
        super(selector);
        this.filter = filter;
    }

    public ColorGridFilter(IGridSelector selector, IColorFilter filter, int scope) {
        super(selector);
        this.filter = filter;
        this.scope = scope;
    }

    public FormattedCell filterFormat(FormattedCell cell) {

        if ((scope & FONT) != 0) {
            short[] fc = cell.getFont().getFontColor();
            if (fc == null) {
                fc = IColorFilter.BLACK;
            }
            cell.getFont().setFontColor(filter.filterColor(fc));
        }

        CellStyle style = cell.getStyle();
        if ((scope & BACKGROUND) != 0) {
            short[] bcg = style.getFillBackgroundColor();
            if (bcg == null) {
                bcg = IColorFilter.WHITE;
            }

            style.setFillBackgroundColor(filter.filterColor(bcg));

            short[] fg = style.getFillForegroundColor();

            if (fg == null) {
                fg = IColorFilter.WHITE;
            }

            style.setFillForegroundColor(filter.filterColor(fg));
        }

        if ((scope & BORDERS) != 0) {
            short[][] bb = style.borderRGB;

            if (bb != null) {
                for (int i = 0; i < bb.length; i++) {
                    bb[i] = filter.filterColor(bb[i]);
                }
            }

        }

        return cell;
    }

}
