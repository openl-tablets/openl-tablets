package org.openl.rules.ui.tablewizard.util;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.util.StringUtils;
import org.richfaces.json.JSONException;
import org.richfaces.json.JSONObject;

public class HTMLToExelStyleCoverter {
    private final static String TOP = "Top";
    private final static String RIGHT = "Right";
    private final static String BOTTOM = "Bottom";
    private final static String LEFT = "Left";

    JSONObject style;

    static public short getBackgroundColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("backgroundColor", style, workbook);
    }

    static public short getBorderTop(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.TOP);
    }

    static public short getTopBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderTopColor", style, workbook);
    }

    static public short getBorderRight(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.RIGHT);
    }

    static public short getRightBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderRightColor", style, workbook);
    }

    static public short getBorderBottom(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.BOTTOM);
    }

    static public short getBottomBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderBottomColor", style, workbook);
    }

    static public short getBorderLeft(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.LEFT);
    }

    static public short getLeftBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderLeftColor", style, workbook);
    }

    static public short getAlignment(JSONObject style) {
        try {
            if (!style.isNull("textAlign")) {
                String textAlign = style.getString("textAlign");

                if ("center".equalsIgnoreCase(textAlign)) {
                    return HSSFCellStyle.ALIGN_CENTER;
                } else if ("left".equalsIgnoreCase(textAlign)) {
                    return HSSFCellStyle.ALIGN_LEFT;
                } else if ("right".equalsIgnoreCase(textAlign)) {
                    return HSSFCellStyle.ALIGN_RIGHT;
                }

                return HSSFCellStyle.ALIGN_LEFT;
            }
        } catch (JSONException e) {
            return HSSFCellStyle.ALIGN_LEFT;
        }

        return 0;
    }

    static public Font getFont(JSONObject style, Workbook wb) {
        short boldWeight = Font.BOLDWEIGHT_NORMAL;
        short color = HSSFColor.BLACK.index;
        short fontHeight = 10*20;
        String name = "Arial";
        boolean italic = false;
        boolean strikeout = false;
        short typeOffset = Font.SS_NONE;
        byte underline = Font.U_NONE; 

        try {
            if (!style.isNull("fontWeight")) {
                String fontWeight = style.getString("fontWeight");

                if ("bold".equalsIgnoreCase(fontWeight)) {
                    boldWeight = Font.BOLDWEIGHT_BOLD;
                }
            }

            if (!style.isNull("fontFamily")) {
                String fontFamily = style.getString("fontFamily");

                if (StringUtils.isNotEmpty(fontFamily)) {
                    name = fontFamily;
                }
            }

            if (!style.isNull("color")) {
                String rgbColor = style.getString("color");

                if(StringUtils.isNotEmpty(rgbColor)) {
                    color = getColorIndex(stringRGBToShort(rgbColor), wb);
                }
            }

            if (!style.isNull("fontSize")) {
                String fontSize = style.getString("fontSize");

                if (fontSize.indexOf("px") > -1) {
                    fontSize = fontSize.replace("px", "");
                }

                if (StringUtils.isNotEmpty(fontSize)) {
                    fontHeight = Short.parseShort(fontSize);

                    //convert to px
                    fontHeight = (short) ((fontHeight/1.2) * 20);
                }

            }

            if (!style.isNull("fontStyle")) {
                String fontStyle = style.getString("fontStyle");

                if ("italic".equalsIgnoreCase(fontStyle)) {
                    italic = true;
                }
            }
            //TODO add initialization of strikeout, typeOffset, underline
        } catch (JSONException e) {

        }

        Font font = wb.findFont(boldWeight, color, fontHeight, name, italic, strikeout, typeOffset, underline);

        if (font == null) {
            font = wb.createFont();

            font.setColor(color);
            font.setBoldweight(boldWeight);
            font.setFontHeight(fontHeight);
            font.setFontName(name);
            font.setItalic(italic);
            font.setStrikeout(strikeout);
            font.setTypeOffset(typeOffset);
            font.setUnderline(underline);
        } 

        return font;
    }

    static private short borderStyleHtmlToExel(String borderStyle, int size) {
        if ("solid".equalsIgnoreCase(borderStyle)) {
            if (size > 2) {
                return CellStyle.BORDER_THICK;
            } else if (size > 0) {
                return CellStyle.BORDER_THIN;
            } else {
                return CellStyle.BORDER_NONE;
            }
            //TODO add code for dotted and double border
        }

        return 0;
    }

    static private short getColorIndex(short[] rgbColor, Workbook workbook) {
        if (rgbColor != null) {
            HSSFWorkbook hssfWorkbook = (HSSFWorkbook) workbook;
            HSSFPalette palette = hssfWorkbook.getCustomPalette();

            HSSFColor hssfColor = palette.findColor((byte) rgbColor[0], (byte) rgbColor[1], (byte) rgbColor[2]);
            if (hssfColor == null ) {
                try {
                    hssfColor = palette.addColor((byte) rgbColor[0], (byte) rgbColor[1], (byte) rgbColor[2]);
                } catch (Exception e) {
                    HSSFColor similarColor = palette.findSimilarColor((int)rgbColor[0], (int)rgbColor[1], (int)rgbColor[2]);
                    palette.setColorAtIndex(similarColor.getIndex(), (byte) rgbColor[0], (byte) rgbColor[1], (byte) rgbColor[2]);
                    hssfColor = palette.getColor(similarColor.getIndex());
                }
            }

            return hssfColor.getIndex();
        }

        return HSSFColor.WHITE.index;
    }

    private static short[] stringRGBToShort(String rgbColor) {
        short[] returnRGB = null;

        rgbColor = rgbColor.replace("rgb(", "");
        rgbColor = rgbColor.replace(")", "");

        String[] rgb = rgbColor.split(",");

        if (rgb.length == 3) {
            returnRGB = new short[3];

            returnRGB[0] = Short.parseShort(rgb[0].trim());
            returnRGB[1] = Short.parseShort(rgb[1].trim());
            returnRGB[2] = Short.parseShort(rgb[2].trim());
        }

        return returnRGB;
    }

    private static byte[] shortToByte(short[] rgbColor) {
        byte rgb[] = new byte[3];

        for (int i = 0; i < 3; i++) {
            rgb[i] = (byte) (rgbColor[i] & 0xFF);
        }

        return rgb;
    }

    private static short getBorder(JSONObject style, String position) {
        String borderType = "border"+position+"Style";
        String borderSize = "border"+position+"Width";

        if (!style.isNull(borderType)) {
            try {
                int size = 0;
                if (!style.isNull(borderSize)) {
                    String sizeStr = style.getString(borderSize);

                    if (sizeStr.length() > 0) {
                        sizeStr = sizeStr.replace("px", "");
                        size = Integer.parseInt(sizeStr);
                    }
                }

                return borderStyleHtmlToExel(style.getString(borderType), size);
            } catch (JSONException e) {
                return CellStyle.BORDER_NONE;
            }
        }

        return CellStyle.BORDER_NONE;
    }

    public static XSSFColor getXSSFBackgroundColor(JSONObject style, XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("backgroundColor", style, workbook);
    }

    public static XSSFColor getXSSFTopBorderColor(JSONObject style, XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderTopColor", style, workbook);
    }

    public static XSSFColor getXSSFRightBorderColor(JSONObject style, XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderRightColor", style, workbook);
    }

    public static XSSFColor getXSSFBottomBorderColor(JSONObject style, XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderBottomColor", style, workbook);
    }

    public static XSSFColor getXSSFLeftBorderColor(JSONObject style, XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderLeftColor", style, workbook);
    }
    
    public static XSSFColor getXSSFColorByHtmlStyleName(String styleName, JSONObject style, XSSFWorkbook workbook) {
        if (!style.isNull(styleName)) {
            try {
                if (StringUtils.isNotEmpty(style.getString(styleName))) {
                    return getXSSFColor(stringRGBToShort(style.getString(styleName)), workbook);
                } else {
                    return new XSSFColor(new byte[]{0,0,0});
                }
            } catch (JSONException e) {
                return new XSSFColor(new byte[]{0,0,0});
            }
        }

        return new XSSFColor(new byte[]{0,0,0});
    }

    private static XSSFColor getXSSFColor(short[] rgb, XSSFWorkbook workbook) {
       XSSFColor color = new XSSFColor(shortToByte(rgb));
        return color;
    }

    public static short getColorByHtmlStyleName(String styleName, JSONObject style, Workbook workbook) {
        if (!style.isNull(styleName)) {
            try {
                return getColorIndex(stringRGBToShort(style.getString(styleName)), workbook);
            } catch (JSONException e) {
                return HSSFColor.WHITE.index;
            }
        }

        return HSSFColor.WHITE.index;
    }

    public static Font getXSSFFont(JSONObject style, XSSFWorkbook workbook) {
        short boldWeight = Font.BOLDWEIGHT_NORMAL;
        // Use indexed color instead of Color.BLACK because of the bug https://issues.apache.org/bugzilla/show_bug.cgi?id=52079
        XSSFColor color = null;
        short indexedColor = Font.COLOR_NORMAL;

        short fontHeight = 10*20;
        String name = "Arial";
        boolean italic = false;
        boolean strikeout = false;
        short typeOffset = Font.SS_NONE;
        byte underline = Font.U_NONE; 

        try {
            if (!style.isNull("fontWeight")) {
                String fontWeight = style.getString("fontWeight");

                if ("bold".equalsIgnoreCase(fontWeight)) {
                    boldWeight = Font.BOLDWEIGHT_BOLD;
                }
            }

            if (!style.isNull("fontFamily")) {
                String fontFamily = style.getString("fontFamily");

                if (StringUtils.isNotEmpty(fontFamily)) {
                    name = fontFamily;
                }
            }

            if (!style.isNull("color")) {
                String rgbColor = style.getString("color");

                if(StringUtils.isNotEmpty(rgbColor)) {
                    color = getXSSFColor(stringRGBToShort(rgbColor), workbook);
                    indexedColor = color.getIndexed();
                }
            }

            if (!style.isNull("fontSize")) {
                String fontSize = style.getString("fontSize");

                if (fontSize.indexOf("px") > -1) {
                    fontSize = fontSize.replace("px", "");
                }

                if (StringUtils.isNotEmpty(fontSize)) {
                    fontHeight = Short.parseShort(fontSize);

                    //convert to px
                    fontHeight = (short) ((fontHeight/1.2) * 20);
                }

            }

            if (!style.isNull("fontStyle")) {
                String fontStyle = style.getString("fontStyle");

                if ("italic".equalsIgnoreCase(fontStyle)) {
                    italic = true;
                }
            }
            //TODO add initialization of strikeout, typeOffset, underline
        } catch (JSONException e) {

        }
        //FIXME equals fronts never find
        XSSFFont font = workbook.findFont(boldWeight, indexedColor, fontHeight, name, italic, strikeout, typeOffset, underline);

        if (font == null || color != null && !font.getXSSFColor().equals(color) ||
                color == null && font.getXSSFColor().getIndexed() != indexedColor) {
            font = workbook.createFont();

            if (color != null) {
                font.setColor(color);
            } else {
                font.setColor(indexedColor);
            }
            font.setBoldweight(boldWeight);
            font.setFontHeight(fontHeight);
            font.setFontName(name);
            font.setItalic(italic);
            font.setStrikeout(strikeout);
            font.setTypeOffset(typeOffset);
            font.setUnderline(underline);
        }

        return font;
    }
}
