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

    int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;
    
    //----------------------------ALIGNMENTS-------------------------
    /**
     * general (normal) horizontal alignment
     */

    short ALIGN_GENERAL = 0x0;

    /**
     * left-justified horizontal alignment
     */

    short ALIGN_LEFT = 0x1;

    /**
     * center horizontal alignment
     */

    short ALIGN_CENTER = 0x2;

    /**
     * right-justified horizontal alignment
     */

    short ALIGN_RIGHT = 0x3;

    /**
     * fill? horizontal alignment
     */

    short ALIGN_FILL = 0x4;

    /**
     * justified horizontal alignment
     */

    short ALIGN_JUSTIFY = 0x5;

    /**
     * center-selection? horizontal alignment
     */

    short ALIGN_CENTER_SELECTION = 0x6;

    /**
     * top-aligned vertical alignment
     */

    short VERTICAL_TOP = 0x0;

    /**
     * center-aligned vertical alignment
     */

    short VERTICAL_CENTER = 0x1;

    /**
     * bottom-aligned vertical alignment
     */

    short VERTICAL_BOTTOM = 0x2;

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

    short VERTICAL_JUSTIFY = 0x3;
    
    //----------------------------END ALIGNMENTS-------------------------
    
    
    //----------------------------BORDERS-------------------------
    /**
     * No border
     */

    short BORDER_NONE = 0x0;

    /**
     * Thin border
     */

    short BORDER_THIN = 0x1;

    /**
     * Medium border
     */

    short BORDER_MEDIUM = 0x2;

    /**
     * dash border
     */

    short BORDER_DASHED = 0x3;

    /**
     * dot border
     */

    short BORDER_DOTTED = 0x4;

    /**
     * Thick border
     */

    short BORDER_THICK = 0x5;

    /**
     * double-line border
     */

    short BORDER_DOUBLE = 0x6;

    /**
     * hair-line border
     */

    short BORDER_HAIR = 0x7;

    /**
     * Medium dashed border
     */

    short BORDER_MEDIUM_DASHED = 0x8;

    /**
     * dash-dot border
     */

    short BORDER_DASH_DOT = 0x9;

    /**
     * medium dash-dot border
     */

    short BORDER_MEDIUM_DASH_DOT = 0xA;

    /**
     * dash-dot-dot border
     */

    short BORDER_DASH_DOT_DOT = 0xB;

    /**
     * medium dash-dot-dot border
     */

    short BORDER_MEDIUM_DASH_DOT_DOT = 0xC;

    /**
     * slanted dash-dot border
     */

    short BORDER_SLANTED_DASH_DOT = 0xD;
    
    //----------------------------END BORDERS-------------------------

    /** No background */
    short NO_FILL = 0;

    /** Solidly filled */
    short SOLID_FOREGROUND = 1;

    /** Small fine dots */
    short FINE_DOTS = 2;

    /** Wide dots */
    short ALT_BARS = 3;

    /** Sparse dots */
    short SPARSE_DOTS = 4;

    /** Thick horizontal bands */
    short THICK_HORZ_BANDS = 5;

    /** Thick vertical bands */
    short THICK_VERT_BANDS = 6;

    /** Thick backward facing diagonals */
    short THICK_BACKWARD_DIAG = 7;

    /** Thick forward facing diagonals */
    short THICK_FORWARD_DIAG = 8;

    /** Large spots */
    short BIG_SPOTS = 9;

    /** Brick-like layout */
    short BRICKS = 10;
    /** Thin horizontal bands */
    short THIN_HORZ_BANDS = 11;
    /** Thin vertical bands */
    short THIN_VERT_BANDS = 12;
    /** Thin backward diagonal */
    short THIN_BACKWARD_DIAG = 13;
    /** Thin forward diagonal */
    short THIN_FORWARD_DIAG = 14;
    /** Squares */
    short SQUARES = 15;
    /** Diamonds */
    short DIAMONDS = 16;
    
    /**
     * 
     * @return RGB colors for all border sides.
     */
    short[][] getBorderRGB();
    
    /**
     * 
     * @return styles for all border sides.
     */
    short[] getBorderStyle();
    
    /**
     * get the background fill color
     * @return fill color
     */
    short[] getFillBackgroundColor();
    
    /**
     * get the foreground fill color
     * @return fill color
     */
    short[] getFillForegroundColor();

    short getFillBackgroundColorIndex();

    short getFillForegroundColorIndex();
    
    short getFillPattern();

    /**
     * get the type of horizontal alignment for the cell
     * @return align - the type of alignment
     * @see #ALIGN_GENERAL
     * @see #ALIGN_LEFT
     * @see #ALIGN_CENTER
     * @see #ALIGN_RIGHT
     * @see #ALIGN_FILL
     * @see #ALIGN_JUSTIFY
     * @see #ALIGN_CENTER_SELECTION
     */
    int getHorizontalAlignment();
    
    /**
     * get the number of spaces to indent the text in the cell
     * @return indent - number of spaces
     * 
     * TODO: rename to getIndent() (wrong spelling)
     */
    int getIdent();
    
    /**
     * get the degree of rotation for the text in the cell
     * @return rotation degrees (between -90 and 90 degrees)
     */
    int getRotation();

    /**
     * get the type of vertical alignment for the cell
     * @return align the type of alignment
     * @see #VERTICAL_TOP
     * @see #VERTICAL_CENTER
     * @see #VERTICAL_BOTTOM
     * @see #VERTICAL_JUSTIFY
     */
    int getVerticalAlignment();
    
    /**
     * get whether the text should be wrapped
     * @return wrap text or not
     */
    boolean isWrappedText();

}
