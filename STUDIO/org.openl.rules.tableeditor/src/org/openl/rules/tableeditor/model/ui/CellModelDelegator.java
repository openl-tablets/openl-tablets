package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.filters.IColorFilter;

public class CellModelDelegator implements ICellModel {

    private CellModel model;

    public CellModelDelegator(CellModel model) {
        this.model = model;
    }

    public BorderStyle[] getBorderStyle() {
        return model.getBorderStyle();
    }

    public int getColspan() {
        return model.getColspan();
    }

    public String getContent(boolean showFormulas) {
        return model.getContent(showFormulas);
    }

    public ICellFont getFont() {
        return model.getFont();
    }

    public int getIndent() {
        return model.getIndent();
    }

    public CellModel getModel() {
        return model;
    }

    public short[] getRgbBackground() {
        return model.getRgbBackground();
    }

    public int getRowspan() {
        return model.getRowspan();
    }

    public boolean isReal() {
        return false;
    }

    public void setBorderStyle(BorderStyle[] borderStyle) {
        model.setBorderStyle(borderStyle);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.rules.ui.ICellModel#setColorFilter(org.openl.rules.ui.IColorFilter[])
     */
    public void setColorFilter(IColorFilter[] filter) {
        // TODO Auto-generated method stub

    }

    public void setColspan(int colspan) {
        model.setColspan(colspan);
    }

    public void setContent(String content) {
        model.setContent(content);
    }

    public void setFont(ICellFont font) {
        model.setFont(font);
    }

    public void setIndent(int indent) {
        model.setIndent(indent);
    }

    public void setRgbBackground(short[] rgbBackground) {
        model.setRgbBackground(rgbBackground);
    }

    public void setRowspan(int rowspan) {
        model.setRowspan(rowspan);
    }

    public String getFormula() {
        return model.getFormula();
    }

    public boolean hasFormula() {
        return model.hasFormula();
    }

    public void setFormula(String formula) {
        model.setFormula(formula);
    }

    public String getComment() {
        return model.getComment();
    }

    public void setComment(String comment) {
        model.setComment(comment);
    }

}
