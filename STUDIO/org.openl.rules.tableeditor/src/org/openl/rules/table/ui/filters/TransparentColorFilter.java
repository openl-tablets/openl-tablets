/*
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.table.ui.filters;


/**
 * @author snshor
 *
 */
public class TransparentColorFilter implements IColorFilter {

    private short[] filter;
    private double transparency; // 0-1, 1 - completely transparent

    public TransparentColorFilter(int color, double transparency) {
        this.transparency = transparency;
        filter = new short[3];
        filter[0] = (short) ((color >> 16) & 0xff);
        filter[1] = (short) ((color >> 8) & 0xff);
        filter[2] = (short) ((color >> 0) & 0xff);
    }

    public TransparentColorFilter(short[] filter, double transparency) {
        this.filter = filter;
        this.transparency = transparency;
    }

    public short[] filterColor(short[] color) {
        short[] res = new short[3];

        if (color == null) {
            color = BLACK;
        }

        for (int i = 0; i < color.length; i++) {
            res[i] = (short) (color[i] * transparency + filter[i] * (1 - transparency));
            if (res[i] > 255) {
                res[i] = 255;
            }
            if (res[i] < 0) {
                res[i] = 0;
            }
        }

        return res;
    }
}
