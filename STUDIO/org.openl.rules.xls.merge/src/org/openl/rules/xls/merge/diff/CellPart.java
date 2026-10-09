package org.openl.rules.xls.merge.diff;

/**
 * A part of a cell a merge takes from one revision independently of the other parts.
 */
public enum CellPart {

    /**
     * The value or the formula of the cell
     */
    VALUE,
    /**
     * The format, font, alignment, borders and fill of the cell
     */
    STYLE,
    /**
     * The comment of the cell
     */
    COMMENT

}
