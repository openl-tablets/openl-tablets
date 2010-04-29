package org.openl.rules.table;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.IncorrectFormulaException;

public interface ICell {

    String ERROR_VALUE = "#ERROR";

    int getRow();

    int getColumn();

    /**
     * @return Absolute row index inside the sheet.
     */
    int getAbsoluteRow();

    /**
     * @return Absolute column index inside the sheet.
     */
    int getAbsoluteColumn();

    int getWidth();

    int getHeight();

    ICellStyle getStyle();
    
    /**
     * @throws IncorrectFormulaException  <br> Be careful!! When trying to evaluate
     *  an incorrect formula, throws exception.
     */
    Object getObjectValue();

    String getStringValue();

    ICellFont getFont();
    
    /**
     * 
     * @return grid region, if cell belongs to any merged region. In other cases <code>null</code>.
     */
    IGridRegion getRegion();

    String getFormula();

    int getType();

    String getUri();
}
