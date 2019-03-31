package org.openl.rules.tableeditor.model.ui;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.ui.ICellFont;

public class CellModel implements ICellModel {

    private int row;
    private int column;

    private int colspan = 1;
    private int rowspan = 1;

    private int indent = 0;
    private String halign;
    private String valign;
    private short[] rgbBackground;
    private BorderStyle[] borderStyle;

    private static final Map<String, Object> DEFAULT_CELL_STYLES = new HashMap<>();
    static {
        // tableeditor.all.css
        DEFAULT_CELL_STYLES.put("padding", 1);
        DEFAULT_CELL_STYLES.put("font-family", "Franklin Gothic Book");
        DEFAULT_CELL_STYLES.put("font-size", 12);
        DEFAULT_CELL_STYLES.put("color", "#000");
        DEFAULT_CELL_STYLES.put("border-style", "solid");
        DEFAULT_CELL_STYLES.put("border-width", "1px");
        DEFAULT_CELL_STYLES.put("border-color", "#bbd");
        DEFAULT_CELL_STYLES.put("background", "#fff");
    }

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

    public void attributesToHtml(StringBuilder buf, boolean selectErrorCell) {
        if (colspan != 1) {
            buf.append(" colspan=\"").append(colspan).append("\"");
        }
        if (rowspan != 1) {
            buf.append(" rowspan=\"").append(rowspan).append("\"");
        }

        String style = getHtmlStyle(selectErrorCell);

        buf.append(" style=\"").append(style).append("\"");
    }

    public void attributesToHtml(StringBuilder buf) {
        attributesToHtml(buf, false);
    }

    private void borderToHtml(StringBuilder buf) {
        if (borderStyle == null) {
            return;
        }

        String[] bwidth = new String[4];
        for (int i = 0; i < borderStyle.length; i++) {
            int width = (borderStyle[i] == null) ? 0 : borderStyle[i].getWidth();
            bwidth[i] = width + (width != 0 ? "px" : "");
        }
        String widthStr = boxCSStoString(bwidth);
        if (!widthStr.equals(DEFAULT_CELL_STYLES.get("border-width"))) {
            buf.append("border-width:").append(widthStr).append(';');
        }

        String[] styles = new String[4];
        for (int i = 0; i < borderStyle.length; i++) {
            String style;
            if ((borderStyle[i] == null || borderStyle[i].getWidth() == 0) && i != 1) {
                style = (borderStyle[1] == null) ? "none" : borderStyle[1].getStyle();
            } else {
                if (borderStyle[i] == null) {
                    style = "none";
                } else {
                    style = borderStyle[i].getStyle();
                }
            }
            styles[i] = style;
        }
        String styleStr = boxCSStoString(styles);
        if (!styleStr.equals(DEFAULT_CELL_STYLES.get("border-style"))) {
            buf.append("border-style:").append(styleStr).append(';');
        }

        String[] colors = new String[4];
        for (int i = 0; i < borderStyle.length; i++) {
            String color;
            if ((borderStyle[i] == null || borderStyle[i].getWidth() == 0) && i != 1) {
                color = (borderStyle[1] == null) ? "#000" : toHexColor(borderStyle[1].getRgb());
            } else {
                if (borderStyle[i] == null) {
                    color = "#000";
                } else {
                    color = toHexColor(borderStyle[i].getRgb());
                }
            }
            colors[i] = color;
        }
        String colorStr = boxCSStoString(colors);
        if (!colorStr.equals(DEFAULT_CELL_STYLES.get("border-color"))) {
            buf.append("border-color:").append(colorStr).append(";");
        }
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

    @Override
    public BorderStyle[] getBorderStyle() {
        return borderStyle;
    }

    @Override
    public int getColspan() {
        return colspan;
    }

    @Override
    public String getContent(boolean showFormulas) {
        if (showFormulas && hasFormula) {
            return convertContent(formula);
        } else {
            return convertContent(content);
        }
    }

    @Override
    public ICellFont getFont() {
        return font;
    }

    public String getHalign() {
        return halign;
    }

    /**
     * Returns style string for cell.
     *
     * @return style string for cell
     */
    public String getHtmlStyle(boolean selectErrorCell) {
        StringBuilder sb = new StringBuilder();
        if (halign != null) {
            sb.append("text-align:").append(halign).append(";");
        }

        if (valign != null) {
            sb.append("vertical-align:").append(valign).append(";");
        }

        if (width != 0) {
            sb.append("width:").append(width).append("px").append(";");
        }

        if (rgbBackground != null) {
            String rgb = toHexColor(rgbBackground);
            if (!rgb.equals(DEFAULT_CELL_STYLES.get("background"))) {
                sb.append("background:").append(rgb).append(";");
            }
        }

        if (selectErrorCell) {
            sb.append("border: 2px solid red;");
        } else if (borderStyle != null) {
            borderToHtml(sb);
        }

        if (font != null) {
            fontToHtml(font, sb);
        }

        if (indent > 0) {
            sb.append("padding-left:")
                .append((Integer) DEFAULT_CELL_STYLES.get("padding") * 0.063 + indent)
                .append("em")
                .append(";");
        }

        return sb.toString();
    }

    public static void fontToHtml(ICellFont font, StringBuilder buf) {
        if (font == null) {
            return;
        }

        if (font.isUnderlined() || font.isStrikeout()) {
            buf.append("text-decoration:");
            if (font.isUnderlined()) {
                buf.append("underline");
            }
            if (font.isStrikeout()) {
                buf.append("line-through");
            }
            buf.append(";");
        }

        String fontName = font.getName();
        if (!fontName.equals(DEFAULT_CELL_STYLES.get("font-family"))) {
            buf.append("font-family:").append(fontName).append(";");
        }
        int fontSize = font.getSize() + 2;
        if (fontSize != (Integer) DEFAULT_CELL_STYLES.get("font-size")) {
            buf.append("font-size:").append(fontSize).append(";");
        }
        if (font.isItalic()) {
            buf.append("font-style:italic").append(";");
        }
        if (font.isBold()) {
            buf.append("font-weight:bold").append(";");
        }

        short[] color = font.getFontColor();
        if (color != null) {
            String colorStr = toHexColor(color);
            if (!colorStr.equals(DEFAULT_CELL_STYLES.get("color"))) {
                buf.append("color:").append(colorStr).append(";");
            }
        }
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public int getIndent() {
        return indent;
    }

    @Override
    public short[] getRgbBackground() {
        return rgbBackground;
    }

    @Override
    public int getRowspan() {
        return rowspan;
    }

    public String getValign() {
        return valign;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    /**
     * Set border style for a cell
     *
     * @param bStyle border style for given direction
     * @param dir one of ICellStyle.TOP, ICellStyle.BOTTOM, ICellStyle.LEFT, ICellStyle.RIGHT
     */
    public void setBorderStyle(BorderStyle bStyle, int dir) {
        if (borderStyle == null) {
            borderStyle = new BorderStyle[4];
        }
        borderStyle[dir] = bStyle;
    }

    @Override
    public void setBorderStyle(BorderStyle[] borderStyle) {
        this.borderStyle = borderStyle;
    }

    @Override
    public void setColspan(int colspan) {
        this.colspan = colspan;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public void setFont(ICellFont font) {
        this.font = font;
    }

    public void setHalign(String halign) {
        this.halign = halign;
    }

    @Override
    public void setIndent(int indent) {
        this.indent = indent;
    }

    @Override
    public void setRgbBackground(short[] rgbBackground) {
        this.rgbBackground = rgbBackground;
    }

    @Override
    public void setRowspan(int rowspan) {
        this.rowspan = rowspan;
    }

    public void setValign(String valign) {
        this.valign = valign;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean hasFormula() {
        return hasFormula;
    }

    @Override
    public void setFormula(String formula) {
        this.formula = "=" + formula;
        hasFormula = true;
    }

    @Override
    public String getFormula() {
        return formula;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    private static String boxCSStoString(String[] values) {
        String result;

        boolean evenSame = values[1].equals(values[3]);
        boolean pairSame = evenSame && values[0].equals(values[2]);
        boolean allSame = pairSame && values[0].equals(values[1]);

        if (allSame) {
            result = values[0];
        } else if (pairSame) {
            result = values[0] + ' ' + values[1];
        } else if (evenSame) {
            result = values[0] + ' ' + values[1] + ' ' + values[2];
        } else {
            result = values[0] + ' ' + values[1] + ' ' + values[2] + ' ' + values[3];
        }
        return result;
    }

    private static String toHex(short x) {
        String s = Integer.toHexString(x);
        if (s.length() == 1) {
            return "0" + s;
        } else if (s.length() == 2) {
            return s;
        }
        return s.substring(s.length() - 2);
    }

    private static String toHexColor(short[] x) {
        if (x == null) {
            return "#000";
        }

        String hex1 = toHex(x[0]);
        String hex2 = toHex(x[1]);
        String hex3 = toHex(x[2]);

        boolean dig3hex = (hex1.charAt(0) == hex1.charAt(1)) && (hex2.charAt(0) == hex2.charAt(1)) && (hex3
            .charAt(0) == hex3.charAt(1));

        return "#" + (dig3hex ? hex1.charAt(0) : hex1) + (dig3hex ? hex2.charAt(0) : hex2) + (dig3hex ? hex3.charAt(0)
                                                                                                      : hex3);
    }
}
