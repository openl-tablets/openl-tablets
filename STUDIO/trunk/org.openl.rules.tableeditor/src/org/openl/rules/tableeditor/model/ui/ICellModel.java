package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.table.ui.ICellFont;

public interface ICellModel {

    public BorderStyle[] getBorderStyle();

    public int getColspan();

    public String getContent(boolean showFormulas);

    public ICellFont getFont();

    public int getIdent();

    public short[] getRgbBackground();

    public int getRowspan();

    public boolean isReal();

    public void setBorderStyle(BorderStyle[] borderStyle);

    public void setColspan(int colspan);

    public void setContent(String content);

    public void setFont(ICellFont font);

    public void setIdent(int ident);

    public void setRgbBackground(short[] rgbBackground);

    public void setRowspan(int rowspan);

    public void toHtmlString(StringBuilder buf, TableModel model);

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

}