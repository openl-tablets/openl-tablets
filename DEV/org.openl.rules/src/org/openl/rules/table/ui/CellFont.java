/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

import lombok.Getter;
import lombok.Setter;

/**
 * @author snshor
 */
public class CellFont implements ICellFont {

    @Getter
    @Setter
    private short[] fontColor;

    @Getter
    @Setter
    private int size;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private boolean italic;

    @Getter
    @Setter
    private boolean bold;

    @Getter
    @Setter
    private boolean underlined;

    @Getter
    @Setter
    private boolean strikeout;

    public CellFont(ICellFont cf) {
        if (cf == null) {
            name = "arial";
            size = 9;
            return;
        }

        fontColor = cf.getFontColor();

        size = cf.getSize();

        name = cf.getName();

        italic = cf.isItalic();
        bold = cf.isBold();
        underlined = cf.isUnderlined();
        strikeout = cf.isStrikeout();
    }

}
