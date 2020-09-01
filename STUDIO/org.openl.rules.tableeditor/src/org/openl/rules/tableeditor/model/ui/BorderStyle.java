/**
 * Created Apr 1, 2007
 */
package org.openl.rules.tableeditor.model.ui;

public class BorderStyle {

    public static final BorderStyle NONE = new BorderStyle(1, "solid", new short[] { 0xBB, 0xBB, 0xDD });

    private int width;

    private String style = "none";

    private short[] rgb = { 0, 0, 0 };

    public BorderStyle() {
    }

    public BorderStyle(int width, String style, short[] rgb) {
        this.width = width;
        this.style = style;
        this.rgb = rgb;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public short[] getRgb() {
        return rgb;
    }

    public void setRgb(short[] rgb) {
        this.rgb = rgb;
    }

}
