/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

/**
 * @author snshor
 *
 */
public class CellStyle implements ICellStyle {

    int horizontalAlignment = ALIGN_GENERAL;

    int verticalAlignment = ALIGN_GENERAL;

    short[] fillBackgroundColor;

    short[] fillForegroundColor;
    String textFormat;

    short[] borderStyle;

    short[][] borderRGB;
    int ident;

    boolean wrappedText;

    int rotation;

    public CellStyle(ICellStyle cs) {
        if (cs == null) {
            return;
        }

        horizontalAlignment = cs.getHorizontalAlignment();

        verticalAlignment = cs.getVerticalAlignment();

        fillBackgroundColor = cs.getFillBackgroundColor();
        fillForegroundColor = cs.getFillForegroundColor();

        textFormat = cs.getTextFormat();

        borderStyle = cs.getBorderStyle();
        borderRGB = cs.getBorderRGB();

        ident = cs.getIdent();

        wrappedText = cs.isWrappedText();

        rotation = cs.getRotation();
    }

    public short[][] getBorderRGB() {
        return borderRGB;
    }

    public short[] getBorderStyle() {
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

    public String getTextFormat() {
        return textFormat;
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

    public void setBorderStyle(short[] borderStyle) {
        this.borderStyle = borderStyle;
    }

    public void setFillBackgroundColor(short[] fillBackgroundColor) {
        this.fillBackgroundColor = fillBackgroundColor;
    }

    public void setFillForegroundColor(short[] fillForegroundColor) {
        this.fillForegroundColor = fillForegroundColor;
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

    public void setTextFormat(String textFormat) {
        this.textFormat = textFormat;
    }

    public void setVerticalAlignment(int verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public void setWrappedText(boolean wrappedText) {
        this.wrappedText = wrappedText;
    }

}
