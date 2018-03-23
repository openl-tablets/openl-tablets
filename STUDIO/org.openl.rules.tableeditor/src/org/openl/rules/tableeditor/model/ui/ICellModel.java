package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.table.ui.ICellFont;

public interface ICellModel {

    BorderStyle[] getBorderStyle();

    int getColspan();

    String getContent(boolean showFormulas);

    ICellFont getFont();

    int getIndent();

    short[] getRgbBackground();

    int getRowspan();

    boolean isReal();

    void setBorderStyle(BorderStyle[] borderStyle);

    void setColspan(int colspan);

    void setContent(String content);

    void setFont(ICellFont font);

    void setIndent(int indent);

    void setRgbBackground(short[] rgbBackground);

    void setRowspan(int rowspan);

    /**
     * @return true if cell contains formula.
     */
    boolean hasFormula();

    /**
     * Sets formula of cell.
     * 
     * @param formula Formula of cell.
     */
    void setFormula(String formula);

    /**
     * @return Formula of cell if it is contained in cell.
     */
    String getFormula();

    String getComment();

    void setComment(String comment);

}