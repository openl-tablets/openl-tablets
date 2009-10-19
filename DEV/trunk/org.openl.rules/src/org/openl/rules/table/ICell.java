package org.openl.rules.table;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.IncorrectFormulaException;

public interface ICell {

    public static final String ERROR_VALUE = "#ERROR";

    int getRow();

    int getColumn();

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

    IGridRegion getRegion();

    String getFormula();

    int getType();

    String getUri();
}
