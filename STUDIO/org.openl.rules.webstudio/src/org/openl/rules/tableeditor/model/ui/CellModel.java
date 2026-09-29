package org.openl.rules.tableeditor.model.ui;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.table.ui.ICellFont;

/**
 * A cell as it is laid out: where it stands, how far it reaches, and how it is styled.
 */
@Getter
@Setter
public final class CellModel implements ICellModel {

    private final int row;
    private final int column;

    private int colspan = 1;
    private int rowspan = 1;

    private int indent;
    private String halign;
    private String valign;
    private short[] rgbBackground;
    private BorderStyle[] borderStyle;

    private ICellFont font;

    public CellModel(int row, int column) {
        this.row = row;
        this.column = column;
    }

    /**
     * Set border style for a cell
     *
     * @param bStyle border style for given direction
     * @param dir    one of ICellStyle.TOP, ICellStyle.BOTTOM, ICellStyle.LEFT, ICellStyle.RIGHT
     */
    public void setBorderStyle(BorderStyle bStyle, int dir) {
        if (borderStyle == null) {
            borderStyle = new BorderStyle[4];
        }
        borderStyle[dir] = bStyle;
    }
}
