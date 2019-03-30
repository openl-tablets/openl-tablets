/*
 * Created on Jul 1, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.table.ui;

import org.apache.poi.ss.usermodel.*;

/**
 * @author snshor Temporary we copy POI constants in here, we will provide more complicated mapping once we start using
 *         other libraries, if ever
 */
public interface ICellStyle {

    int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;

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
     * 
     * @return fill color
     */
    short[] getFillBackgroundColor();

    /**
     * get the foreground fill color
     * 
     * @return fill color
     */
    short[] getFillForegroundColor();

    short getFillBackgroundColorIndex();

    short getFillForegroundColorIndex();

    FillPatternType getFillPattern();

    /**
     * get the type of horizontal alignment for the cell
     * 
     * @return align - the type of alignment
     */
    HorizontalAlignment getHorizontalAlignment();

    /**
     * get the number of spaces to indent the text in the cell
     * 
     * @return indent - number of spaces
     */
    int getIndent();

    /**
     * get the degree of rotation for the text in the cell
     * 
     * @return rotation degrees (between -90 and 90 degrees)
     */
    int getRotation();

    /**
     * get the type of vertical alignment for the cell
     * 
     * @return align the type of alignment
     */
    VerticalAlignment getVerticalAlignment();

    /**
     * get whether the text should be wrapped
     * 
     * @return wrap text or not
     */
    boolean isWrappedText();

    /**
     * Get the index of the data format. Built-in formats are defined in {@link BuiltinFormats}.
     * 
     * @see DataFormat
     */
    short getFormatIndex();

    /**
     * Get the format string
     */
    String getFormatString();
}
