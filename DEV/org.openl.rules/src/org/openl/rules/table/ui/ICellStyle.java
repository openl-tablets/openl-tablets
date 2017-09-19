/*
 * Created on Jul 1, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.table.ui;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;

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
     * vertically justified vertical alignment
     */

    short VERTICAL_JUSTIFY = 0x3;
    
    //----------------------------END ALIGNMENTS-------------------------
    
    /**
     * 
     * @return RGB colors for all border sides.
     */
    short[][] getBorderRGB();
    
    /**
     * 
     * @return styles for all border sides.
     */
    BorderStyle[] getBorderStyle();
    
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

    FillPatternType getFillPattern();

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
