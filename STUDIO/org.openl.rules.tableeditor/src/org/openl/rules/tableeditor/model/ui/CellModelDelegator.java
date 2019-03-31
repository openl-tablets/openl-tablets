package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.filters.IColorFilter;

public class CellModelDelegator implements ICellModel {

    private CellModel model;

    public CellModelDelegator(CellModel model) {
        this.model = model;
    }

    @Override
    public BorderStyle[] getBorderStyle() {
        return model.getBorderStyle();
    }

    @Override
    public int getColspan() {
        return model.getColspan();
    }

    @Override
    public String getContent(boolean showFormulas) {
        return model.getContent(showFormulas);
    }

    @Override
    public ICellFont getFont() {
        return model.getFont();
    }

    @Override
    public int getIndent() {
        return model.getIndent();
    }

    public CellModel getModel() {
        return model;
    }

    @Override
    public short[] getRgbBackground() {
        return model.getRgbBackground();
    }

    @Override
    public int getRowspan() {
        return model.getRowspan();
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
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

    @Override
    public void setColspan(int colspan) {
        model.setColspan(colspan);
    }

    @Override
    public void setContent(String content) {
        model.setContent(content);
    }

    @Override
    public void setFont(ICellFont font) {
        model.setFont(font);
    }

    @Override
    public void setIndent(int indent) {
        model.setIndent(indent);
    }

    @Override
    public void setRgbBackground(short[] rgbBackground) {
        model.setRgbBackground(rgbBackground);
    }

    @Override
    public void setRowspan(int rowspan) {
        model.setRowspan(rowspan);
    }

    @Override
    public String getFormula() {
        return model.getFormula();
    }

    @Override
    public boolean hasFormula() {
        return model.hasFormula();
    }

    @Override
    public void setFormula(String formula) {
        model.setFormula(formula);
    }

    @Override
    public String getComment() {
        return model.getComment();
    }

    @Override
    public void setComment(String comment) {
        model.setComment(comment);
    }

}
