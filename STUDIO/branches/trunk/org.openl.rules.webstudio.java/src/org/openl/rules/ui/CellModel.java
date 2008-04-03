package org.openl.rules.ui;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.webtools.WebTool;


public class CellModel implements ICellModel {
    static final short[] WHITE = { 255, 255, 255 };

    // public void setColorFilter(IColorFilter[] filter) {
    // this.filter = filter;
    // }
    static final int FONT_COLOR_FILTER_IDX = 0;

    // public void setColorFilter(IColorFilter[] filter) {
    // this.filter = filter;
    // }
    static final int BGR_COLOR_FILTER_IDX = 1;

    // public void setColorFilter(IColorFilter[] filter) {
    // this.filter = filter;
    // }
    static final int BORDER_COLOR_FILTER_IDX = 2;
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

    // int[] borderWidth;
    //
    // String[] borderStyle;
    //
    // short[][] borderColor;

    // String fontFamily;
    // String fontSize;
    // short[] fontColor;
    // boolean isItalic;
    // boolean
    ICellFont font;
    int width;

    public CellModel(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getColspan() {
        return colspan;
    }

    public void setColspan(int colspan) {
        this.colspan = colspan;
    }

    public String getContent() {
        return convertContent(content);
    }

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

    String convertContent(String content) {
        StringBuilder buf = new StringBuilder(content.length() + 100);

        // if (content.charAt(0) == '<')
        // {
        // int idx = content.indexOf('>');
        // buf.append(content.substring(0, idx)).append(" style=\"");
        // fontToHtml(buf);
        // buf.append('\"');
        // buf.append(content.substring(idx));
        // return buf.toString();
        // }
        boolean startLine = true;
        boolean needIdent = true;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (needIdent) {
                for (int j = 0; j < ident; j++) {
                    buf.append("&nbsp;&nbsp;");
                }
                needIdent = false;
            }

            if ((ch == ' ') && startLine) {
                buf.append("&nbsp;");
                continue;
            }

            if (ch == '\n') {
                startLine = true;
                needIdent = true;
                buf.append("<br>");
                continue;
            }

            buf.append(ch);
            startLine = false;
        }

        return buf.toString();
    }

    public void setContent(String content) {
        this.content = content;
    }

    public short[] getRgbBackground() {
        return rgbBackground;
    }

    public void setRgbBackground(short[] rgbBackground) {
        this.rgbBackground = rgbBackground;
    }

    public int getRowspan() {
        return rowspan;
    }

    public void setRowspan(int rowspan) {
        this.rowspan = rowspan;
    }

    public void toHtmlString(StringBuffer buf, TableModel table) {
        buf.append("<td ");
        atttributesToHtml(buf, table);
        buf.append('>').append("<div ").append(" onMouseDown='clickCell(").append(column)
            .append(',').append(row).append(",event)'").append(" id='c").append(column)
            .append('x').append(row).append("'>").append(getContent())
            .append("</div></td>\n");
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

        // else if (isNumber(content))
        // buf.append(" align=right");
        if (rgbBackground == null) {
            rgbBackground = WHITE;
        }

        // IColorFilter bcgFilter = getColorFilter(BGR_COLOR_FILTER_IDX);

        // short[] color = bcgFilter == null ? rgbBackground :
        // bcgFilter.filterColor(rgbBackground);
        short[] color = rgbBackground;

        buf.append(" bgcolor=" + WebTool.toHexString(color));

        if ((borderStyle != null) || (font != null)) {
            buf.append(" style=\"padding:1px;");
            borderToHtml(buf, table);
            WebTool.fontToHtml(font, buf);
            buf.append('"');
        }
    }

    // /**
    // * @param content2
    // * @return
    // */
    // private static boolean isNumber(String content)
    // {
    // if (content.charAt(0) == '<')
    // return true;
    //
    // try
    // {
    // if (content.endsWith("%"))
    // content = content.substring(0, content.length()-1);
    //
    // Double.parseDouble(content);
    // return true;
    // } catch (Throwable e)
    // {
    // return false;
    // }
    // }
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

            buf.append(' ').append(w);
        }
        buf.append(';');

        buf.append(" border-color:");

        for (int i = 0; i < borderStyle.length; i++) {
            short[] rgb = (borderStyle[i] == null) ? new short[] { 0, 0, 0 }
                : borderStyle[i].rgb;

            buf.append(' ').append(WebTool.toHexString(rgb));
        }
        buf.append(';');
    }

    public boolean isReal() {
        return true;
    }

    public String getHalign() {
        return halign;
    }

    public void setHalign(String halign) {
        this.halign = halign;
    }

    public ICellFont getFont() {
        return font;
    }

    public void setFont(ICellFont font) {
        this.font = font;
    }

    // IColorFilter[] filter = null;
    //
    // IColorFilter getColorFilter(int i)
    // {
    // if (filter == null || filter.length <= i)
    // return null;
    // return filter[i];
    // }

    // public void setTextFilter(ITextFilter textFilter)
    // {
    // this.textFilter = textFilter;
    // }
    // ITextFilter textFilter;
    /**
     * DOCUMENT ME!
     *
     * @return Returns the ident.
     */
    public int getIdent() {
        return ident;
    }

    /**
     * DOCUMENT ME!
     *
     * @param ident The ident to set.
     */
    public void setIdent(int ident) {
        this.ident = ident;
    }

    /**
     * DOCUMENT ME!
     *
     * @param w
     */
    public void setWidth(int w) {
        width = w;
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

    public BorderStyle[] getBorderStyle() {
        return this.borderStyle;
    }

    public void setBorderStyle(BorderStyle[] borderStyle) {
        this.borderStyle = borderStyle;
    }

    public String getValign() {
        return this.valign;
    }

    public void setValign(String valign) {
        this.valign = valign;
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
}
