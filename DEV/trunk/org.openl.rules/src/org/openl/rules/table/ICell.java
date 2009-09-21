package org.openl.rules.table;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

public interface ICell {

    int getRow();
    int getColumn();

    int getWidth();
    int getHeight();

    ICellStyle getStyle();

    Object getObjectValue();
    String getStringValue();

    ICellFont getFont();

    IGridRegion getRegion();

    String getFormula();
    
    int getType();

    String getUri();
}
