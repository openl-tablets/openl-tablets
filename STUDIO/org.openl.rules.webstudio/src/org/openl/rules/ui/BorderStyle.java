/**
 * Created Apr 1, 2007
 */
package org.openl.rules.ui;

/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public class BorderStyle {
    /** DOCUMENT ME! */
    public static final BorderStyle NONE = new BorderStyle(1, "solid",
            new short[] { 0xBB, 0xBB, 0xDD });
    int width = 0;
    String style = "none";
    short[] rgb = { 0, 0, 0 };

    public BorderStyle(int w, String style, short[] rgb) {
        this.width = w;
        this.style = style;
        this.rgb = rgb;
    }

/**
         *
         */
    public BorderStyle() {}
}
