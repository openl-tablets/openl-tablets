/**
 * Created Apr 1, 2007
 */
package org.openl.rules.tableeditor.model.ui;

import lombok.Getter;
import lombok.Setter;

public class BorderStyle {

    public static final BorderStyle NONE = new BorderStyle(1, "solid", new short[]{0xBB, 0xBB, 0xDD});

    @Getter
    @Setter
    private int width;

    @Getter
    @Setter
    private String style = "none";

    @Getter
    @Setter
    private short[] rgb = {0, 0, 0};

    public BorderStyle() {
    }

    public BorderStyle(int width, String style, short[] rgb) {
        this.width = width;
        this.style = style;
        this.rgb = rgb;
    }

}
