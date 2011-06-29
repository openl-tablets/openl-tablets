package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.tableeditor.model.ui.util.HTMLHelper;

public class CellModel implements ICellModel {

    public static final short[] WHITE = { 255, 255, 255 };

    private int row;
    private int column;

    private int colspan = 1;
    private int rowspan = 1;

    private int ident = 0;
    private String halign;
    private String valign;
    private short[] rgbBackground;
    private BorderStyle[] borderStyle;
    private int cellPadding = 1;
    private boolean hasFormula;
    private String formula;

    private String content = "&nbsp;";

    private String comment;

    private ICellFont font;
    private int width;

    static int calcMaxLineLength(String content) {
        int max = 0;
        int from = 0;

        while (true) {
            int idx1 = content.indexOf('\n', from);
            if (idx1 <= 0) {
                return max;
            }
            max = Math.max(max, idx1 - from);
            from = idx1 + 1;
        }
    }

    public CellModel(int row, int column) {
        this.row = row;
        this.column = column;
        hasFormula = false;
    }

    public void atttributesToHtml(StringBuilder buf, TableModel table, boolean selectErrorCell) {
        if (colspan != 1) {
            buf.append(" colspan=\"").append(colspan).append("\"");
        }
        if (rowspan != 1) {
            buf.append(" rowspan=\"").append(rowspan).append("\"");
        }

        String style = getHtmlStyle(table, selectErrorCell);

        buf.append(" style=\"" + style + "\"");
    }

    public void atttributesToHtml(StringBuilder buf, TableModel table) {
        atttributesToHtml(buf, table, false);
    }

    private void borderToHtml(StringBuilder buf, TableModel table) {
        if (borderStyle == null) {
            return;
        }

        buf.append(" border-style: ");
        for (int i = 0; i < borderStyle.length; i++) {
            buf.append((borderStyle[i] == null) ? "none" : borderStyle[i].getStyle());
            buf.append(' ');
        }
        buf.append(';');

        buf.append(" border-width:");

        for (int i = 0; i < borderStyle.length; i++) {
            int w = (borderStyle[i] == null) ? 0 : borderStyle[i].getWidth();

            buf.append(' ').append(w).append("px");
        }
        buf.append(';');

        buf.append(" border-color:");

        for (int i = 0; i < borderStyle.length; i++) {
            short[] rgb = (borderStyle[i] == null) ? new short[] { 0, 0, 0 } : borderStyle[i].getRgb();

            buf.append(' ').append(HTMLHelper.toHexColor(rgb));
        }
        buf.append(';');
    }

    String convertContent(String content) {
        StringBuilder buf = new StringBuilder(content.length() + 100);

        boolean startLine = true;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if ((ch == ' ') && startLine) {
                buf.append("&nbsp;");
                continue;
            }

            if (ch == '\n') {
                startLine = true;
                buf.append("<br>");
                continue;
            }

            buf.append(ch);
            startLine = false;
        }

        return buf.toString();
    }

    public BorderStyle[] getBorderStyle() {
        return borderStyle;
    }

    public int getColspan() {
        return colspan;
    }

    public String getContent(boolean showFormulas) {
        if (showFormulas && hasFormula) {
            return convertContent(formula);
        } else {
            return convertContent(content);
        }
    }

    public ICellFont getFont() {
        return font;
    }

    public String getHalign() {
        return halign;
    }

    /**
     * Returns style string for cell.
     *
     * @param tm
     *
     * @return style string for cell
     */
    public String getHtmlStyle(TableModel tm, boolean selectErrorCell) {
        StringBuilder sb = new StringBuilder();
        if (halign != null) {
            sb.append("text-align:" + halign + ";");
        }

        if (valign != null) {
            sb.append("vertical-align:" + valign + ";");
        }

        if (width != 0) {
            sb.append("width:" + width + "px" + ";");
        }

        if (rgbBackground == null) {
            rgbBackground = WHITE;
        }

        short[] color = rgbBackground;
        sb.append("background-color:" + HTMLHelper.toRgbColor(color) + ";");

        if (borderStyle != null || font != null) {
            sb.append("padding:" + String.valueOf(cellPadding) + "px" + ";");
            if (selectErrorCell) {
                sb.append(" border: 2px solid red;");
            } else {
                borderToHtml(sb, tm);
            }
            fontToHtml(font, sb);
        }

        if (ident > 0) {
            sb.append("padding-left:" + (cellPadding * 0.063 + ident) + "em" + ";");
        }

        return sb.toString();
    }

    public static StringBuilder fontToHtml(ICellFont font, StringBuilder buf) {
        if (font == null) {
            return buf;
        }

        if (font.isUnderlined()) {
            buf.append("text-decoration: underline;");
        }

        buf.append("font-family: ").append(font.getName());
        buf.append("; font-size: ").append(font.getSize() + 2);
        if (font.isItalic()) {
            buf.append("; font-style: italic");
        }
        if (font.isBold()) {
            buf.append("; font-weight: bold");
        }

        short[] color = font.getFontColor();

        buf.append("; color: " + HTMLHelper.toHexColor(color) + ";");

        return buf;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public int getIdent() {
        return ident;
    }

    public short[] getRgbBackground() {
        return rgbBackground;
    }

    public int getRowspan() {
        return rowspan;
    }

    public String getValign() {
        return valign;
    }

    public boolean isReal() {
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param bStyle
     * @param dir
     */
    public void setBorderStyle(BorderStyle bStyle, int dir) {
        if (borderStyle == null) {
            borderStyle = new BorderStyle[4];
        }
        borderStyle[dir] = bStyle;
    }

    public void setBorderStyle(BorderStyle[] borderStyle) {
        this.borderStyle = borderStyle;
    }

    public void setColspan(int colspan) {
        this.colspan = colspan;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setFont(ICellFont font) {
        this.font = font;
    }

    public void setHalign(String halign) {
        this.halign = halign;
    }

    public void setIdent(int ident) {
        this.ident = ident;
    }

    public void setRgbBackground(short[] rgbBackground) {
        this.rgbBackground = rgbBackground;
    }

    public void setRowspan(int rowspan) {
        this.rowspan = rowspan;
    }

    public void setValign(String valign) {
        this.valign = valign;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean hasFormula() {
        return hasFormula;
    }

    public void setFormula(String formula) {
        this.formula = "=" + formula;
        hasFormula = true;
    }

    public String getFormula() {
        return formula;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
