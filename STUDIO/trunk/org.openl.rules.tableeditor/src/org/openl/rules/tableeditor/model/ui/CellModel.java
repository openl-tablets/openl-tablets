package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.webtools.WebTool;

public class CellModel implements ICellModel {

    static final short[] WHITE = { 255, 255, 255 };

    int row;
    int column;
    int ident = 0;
    String content = "&nbsp;";
    int colspan = 1;
    int rowspan = 1;
    String halign;
    String valign;
    short[] rgbBackground;
    BorderStyle[] borderStyle;
    private boolean hasFormula;
    private String formula;

    ICellFont font;
    int width;

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

    private void addStyleAttribute(StringBuffer style, String attribute) {
        if (style.length() == 0) {
            style.append(" style=\"");
        }
        if (style.charAt(style.length() - 1) != ';') {
            style.append(";");
        }
        style.append(attribute).append(";");
    }

    public void atttributesToHtml(StringBuffer buf, TableModel table) {
        if (colspan != 1) {
            buf.append(" colspan=").append(colspan);
        }
        if (rowspan != 1) {
            buf.append(" rowspan=").append(rowspan);
        }
        if (halign != null) {
            buf.append(" align=" + halign);
        }
        if (valign != null) {
            buf.append(" valign=" + valign);
        }
        if (width != 0) {
            buf.append(" width=" + width);
        }

        if (rgbBackground == null) {
            rgbBackground = WHITE;
        }

        short[] color = rgbBackground;

        buf.append(" bgcolor=" + WebTool.toHexString(color));

        float cellPadding = 1;
        StringBuffer style = new StringBuffer();
        if ((borderStyle != null) || (font != null)) {
            addStyleAttribute(style, "padding:" + String.valueOf(cellPadding) + "px");
            borderToHtml(style, table);
            WebTool.fontToHtml(font, style);
        }
        if (ident > 0) {
            addStyleAttribute(style, "padding-left:" + (cellPadding * 0.063 + ident) + "em");
        }
        if (style.length() != 0) {
            style.append("\"");
            buf.append(style);
        }
    }

    private void borderToHtml(StringBuffer buf, TableModel table) {
        if (borderStyle == null) {
            return;
        }

        buf.append(" border-style: ");
        for (int i = 0; i < borderStyle.length; i++) {
            buf.append((borderStyle[i] == null) ? "none" : borderStyle[i].style);
            buf.append(' ');
        }
        buf.append(';');

        buf.append(" border-width:");

        for (int i = 0; i < borderStyle.length; i++) {
            int w = (borderStyle[i] == null) ? 0 : borderStyle[i].width;

            buf.append(' ').append(w).append("px");
        }
        buf.append(';');

        buf.append(" border-color:");

        for (int i = 0; i < borderStyle.length; i++) {
            short[] rgb = (borderStyle[i] == null) ? new short[] { 0, 0, 0 } : borderStyle[i].rgb;

            buf.append(' ').append(WebTool.toHexString(rgb));
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
     * Returns style string for cell
     *
     * @param tm
     *
     * @return style string for cell
     */
    public String getHtmlStyle(TableModel tm) {
        StringBuffer sb = new StringBuffer();
        if (halign != null) {
            sb.append("text-align:" + halign + ";");
        }

        if (valign != null) {
            sb.append("vertical-align:" + valign + ";");
        }

        if (width != 0) {
            sb.append("width:" + width + ";");
        }

        if (rgbBackground == null) {
            rgbBackground = WHITE;
        }

        short[] color = rgbBackground;
        sb.append("background-color:" + WebTool.toRgbString(color) + ";");

        if ((borderStyle != null) || (font != null)) {
            borderToHtml(sb, tm);
            WebTool.fontToHtml(font, sb);
        }
        return sb.toString();
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

    public void toHtmlString(StringBuffer buf, TableModel table) {
        buf.append("<td ");
        atttributesToHtml(buf, table);
        //FIXME: is this method deprecated? should formulas be displayed?
        buf.append('>').append("<div ").append(" onMouseDown='clickCell(").append(column).append(',').append(row)
                .append(",event)'").append(" id='c").append(column).append('x').append(row).append("'>").append(
                        getContent(false)).append("</div></td>\n");
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
}
