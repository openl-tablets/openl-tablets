/*
 * Created on Jul 1, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.table.ui;

/**
 * @author snshor Temporary we copy POI constants in here, we will provide more
 *         complicated mapping once we start using other libraries, if ever
 */
public interface ICellStyle {

    static public final int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;

    /**
     * general (normal) horizontal alignment
     */

    public final static short ALIGN_GENERAL = 0x0;

    /**
     * left-justified horizontal alignment
     */

    public final static short ALIGN_LEFT = 0x1;

    /**
     * center horizontal alignment
     */

    public final static short ALIGN_CENTER = 0x2;

    /**
     * right-justified horizontal alignment
     */

    public final static short ALIGN_RIGHT = 0x3;

    /**
     * fill? horizontal alignment
     */

    public final static short ALIGN_FILL = 0x4;

    /**
     * justified horizontal alignment
     */

    public final static short ALIGN_JUSTIFY = 0x5;

    /**
     * center-selection? horizontal alignment
     */

    public final static short ALIGN_CENTER_SELECTION = 0x6;

    /**
     * top-aligned vertical alignment
     */

    public final static short VERTICAL_TOP = 0x0;

    /**
     * center-aligned vertical alignment
     */

    public final static short VERTICAL_CENTER = 0x1;

    /**
     * bottom-aligned vertical alignment
     */

    public final static short VERTICAL_BOTTOM = 0x2;

    /**
     *
     * @return String[12] containing border types [0-3], widths[4-7] and colors
     *         [8-11]
     */
    // String[] cssBorders();
    // static final int BORDER_TYPE=0, BORDER_WIDTH=4, BORDER_COLOR=8;

    /**
     * vertically justified vertical alignment
     */

    public final static short VERTICAL_JUSTIFY = 0x3;

    /**
     * No border
     */

    public final static short BORDER_NONE = 0x0;

    /**
     * Thin border
     */

    public final static short BORDER_THIN = 0x1;

    /**
     * Medium border
     */

    public final static short BORDER_MEDIUM = 0x2;

    /**
     * dash border
     */

    public final static short BORDER_DASHED = 0x3;

    /**
     * dot border
     */

    public final static short BORDER_DOTTED = 0x4;

    /**
     * Thick border
     */

    public final static short BORDER_THICK = 0x5;

    /**
     * double-line border
     */

    public final static short BORDER_DOUBLE = 0x6;

    /**
     * hair-line border
     */

    public final static short BORDER_HAIR = 0x7;

    /**
     * Medium dashed border
     */

    public final static short BORDER_MEDIUM_DASHED = 0x8;

    /**
     * dash-dot border
     */

    public final static short BORDER_DASH_DOT = 0x9;

    /**
     * medium dash-dot border
     */

    public final static short BORDER_MEDIUM_DASH_DOT = 0xA;

    /**
     * dash-dot-dot border
     */

    public final static short BORDER_DASH_DOT_DOT = 0xB;

    /**
     * medium dash-dot-dot border
     */

    public final static short BORDER_MEDIUM_DASH_DOT_DOT = 0xC;

    /**
     * slanted dash-dot border
     */

    public final static short BORDER_SLANTED_DASH_DOT = 0xD;

    /** No background */
    public final static short NO_FILL = 0;

    /** Solidly filled */
    public final static short SOLID_FOREGROUND = 1;

    /** Small fine dots */
    public final static short FINE_DOTS = 2;

    /** Wide dots */
    public final static short ALT_BARS = 3;

    /** Sparse dots */
    public final static short SPARSE_DOTS = 4;

    /** Thick horizontal bands */
    public final static short THICK_HORZ_BANDS = 5;

    /** Thick vertical bands */
    public final static short THICK_VERT_BANDS = 6;

    /** Thick backward facing diagonals */
    public final static short THICK_BACKWARD_DIAG = 7;

    /** Thick forward facing diagonals */
    public final static short THICK_FORWARD_DIAG = 8;

    /** Large spots */
    public final static short BIG_SPOTS = 9;

    /** Brick-like layout */
    public final static short BRICKS = 10;
    /** Thin horizontal bands */
    public final static short THIN_HORZ_BANDS = 11;
    /** Thin vertical bands */
    public final static short THIN_VERT_BANDS = 12;
    /** Thin backward diagonal */
    public final static short THIN_BACKWARD_DIAG = 13;
    /** Thin forward diagonal */
    public final static short THIN_FORWARD_DIAG = 14;
    /** Squares */
    public final static short SQUARES = 15;
    /** Diamonds */
    public final static short DIAMONDS = 16;
    short[][] getBorderRGB();
    /**
     *
     * @return
     */
    short[] getBorderStyle();
    short[] getFillBackgroundColor();
    short[] getFillForegroundColor();
    int getHorizontalAlignment();
    int getIdent();
    int getRotation();
    String getTextFormat();
    int getVerticalAlignment();
    boolean isWrappedText();

}
