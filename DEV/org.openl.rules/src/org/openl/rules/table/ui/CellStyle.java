package org.openl.rules.table.ui;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;

/**
 * @author snshor
 *
 */
public class CellStyle implements ICellStyle {

    private int horizontalAlignment = ALIGN_GENERAL;

    private int verticalAlignment = ALIGN_GENERAL;

    private short[] fillBackgroundColor;

    private short[] fillForegroundColor;

    private short fillBackgroundColorIndex;

    private short fillForegroundColorIndex;

    private FillPatternType fillPattern;

    private BorderStyle[] borderStyle;

    private short[][] borderRGB;
    private int ident;

    private boolean wrappedText;

    private int rotation;

    public CellStyle(ICellStyle cellStyle) {
        if (cellStyle == null) {
            return;
        }

        horizontalAlignment = cellStyle.getHorizontalAlignment();

        verticalAlignment = cellStyle.getVerticalAlignment();

        fillBackgroundColor = cellStyle.getFillBackgroundColor();
        fillForegroundColor = cellStyle.getFillForegroundColor();

        fillBackgroundColorIndex = cellStyle.getFillBackgroundColorIndex();
        fillForegroundColorIndex = cellStyle.getFillForegroundColorIndex();
        fillPattern = cellStyle.getFillPattern();

        borderStyle = cellStyle.getBorderStyle();
        borderRGB = cellStyle.getBorderRGB();

        ident = cellStyle.getIdent();

        wrappedText = cellStyle.isWrappedText();

        rotation = cellStyle.getRotation();
    }

    public short[][] getBorderRGB() {
        return borderRGB;
    }

    public BorderStyle[] getBorderStyle() {
        return borderStyle;
    }

    public short[] getFillBackgroundColor() {
        return fillBackgroundColor;
    }

    public short[] getFillForegroundColor() {
        return fillForegroundColor;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public int getIdent() {
        return ident;
    }

    public int getRotation() {
        return rotation;
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public boolean isWrappedText() {
        return wrappedText;
    }

    public void setBorderRGB(short[][] borderRGB) {
        this.borderRGB = borderRGB;
    }

    public void setBorderStyle(BorderStyle[] borderStyle) {
        this.borderStyle = borderStyle;
    }

    public void setFillBackgroundColor(short[] fillBackgroundColor) {
        this.fillBackgroundColor = fillBackgroundColor;
    }

    public void setFillForegroundColor(short[] fillForegroundColor) {
        this.fillForegroundColor = fillForegroundColor;
    }

    public short getFillBackgroundColorIndex() {
        return fillBackgroundColorIndex;
    }
    
    public short getFillForegroundColorIndex() {
        return fillForegroundColorIndex;
    }
    
    public void setFillBackgroundColorIndex(short fillBackgroundColorIndex) {
        this.fillBackgroundColorIndex = fillBackgroundColorIndex;
    }

    public void setFillForegroundColorIndex(short fillForegroundColorIndex) {
        this.fillForegroundColorIndex = fillForegroundColorIndex;
    }
    
    public FillPatternType getFillPattern() {
        return fillPattern;
    }

    public void setFillPattern(FillPatternType fillPattern) {
        this.fillPattern = fillPattern;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public void setIdent(int ident) {
        this.ident = ident;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public void setVerticalAlignment(int verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public void setWrappedText(boolean wrappedText) {
        this.wrappedText = wrappedText;
    }

}
