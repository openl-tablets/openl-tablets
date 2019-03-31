package org.openl.rules.ui.tablewizard.util;

import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.util.StringUtils;
import org.richfaces.json.JSONException;
import org.richfaces.json.JSONObject;

public class HTMLToExcelStyleCoverter {
    private final static String TOP = "Top";
    private final static String RIGHT = "Right";
    private final static String BOTTOM = "Bottom";
    private final static String LEFT = "Left";

    public static short getBackgroundColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("backgroundColor", style, workbook);
    }

    public static BorderStyle getBorderTop(JSONObject style) {
        return getBorder(style, HTMLToExcelStyleCoverter.TOP);
    }

    public static short getTopBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderTopColor", style, workbook);
    }

    public static BorderStyle getBorderRight(JSONObject style) {
        return getBorder(style, HTMLToExcelStyleCoverter.RIGHT);
    }

    public static short getRightBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderRightColor", style, workbook);
    }

    public static BorderStyle getBorderBottom(JSONObject style) {
        return getBorder(style, HTMLToExcelStyleCoverter.BOTTOM);
    }

    public static short getBottomBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderBottomColor", style, workbook);
    }

    public static BorderStyle getBorderLeft(JSONObject style) {
        return getBorder(style, HTMLToExcelStyleCoverter.LEFT);
    }

    public static short getLeftBorderColor(JSONObject style, Workbook workbook) {
        return getColorByHtmlStyleName("borderLeftColor", style, workbook);
    }

    public static HorizontalAlignment getAlignment(JSONObject style) {
        try {
            if (!style.isNull("textAlign")) {
                String textAlign = style.getString("textAlign");

                if ("center".equalsIgnoreCase(textAlign)) {
                    return HorizontalAlignment.CENTER;
                } else if ("left".equalsIgnoreCase(textAlign)) {
                    return HorizontalAlignment.LEFT;
                } else if ("right".equalsIgnoreCase(textAlign)) {
                    return HorizontalAlignment.RIGHT;
                }

                return HorizontalAlignment.LEFT;
            }
        } catch (JSONException e) {
            return HorizontalAlignment.LEFT;
        }

        return HorizontalAlignment.GENERAL;
    }

    public static Font getFont(JSONObject style, Workbook wb) {
        boolean boldWeight = false;
        short color = HSSFColor.HSSFColorPredefined.BLACK.getIndex();
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
                    boldWeight = true;
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

                if (fontSize.contains("px")) {
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
        } catch (JSONException ignored) {

        }

        Font font = wb.findFont(boldWeight, color, fontHeight, name, italic, strikeout, typeOffset, underline);

        if (font == null) {
            font = wb.createFont();

            font.setColor(color);
            font.setBold(boldWeight);
            font.setFontHeight(fontHeight);
            font.setFontName(name);
            font.setItalic(italic);
            font.setStrikeout(strikeout);
            font.setTypeOffset(typeOffset);
            font.setUnderline(underline);
        } 

        return font;
    }

    static private BorderStyle borderStyleHtmlToExcel(String borderStyle, int size) {
        if ("solid".equalsIgnoreCase(borderStyle)) {
            if (size > 2) {
                return BorderStyle.THICK;
            } else if (size > 0) {
                return BorderStyle.THIN;
            } else {
                return BorderStyle.NONE;
            }
            //TODO add code for dotted and double border
        }

        return BorderStyle.NONE;
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

        return HSSFColor.HSSFColorPredefined.WHITE.getIndex();
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

    private static BorderStyle getBorder(JSONObject style, String position) {
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

                return borderStyleHtmlToExcel(style.getString(borderType), size);
            } catch (JSONException e) {
                return BorderStyle.NONE;
            }
        }

        return BorderStyle.NONE;
    }

    public static XSSFColor getXSSFBackgroundColor(JSONObject style,
            XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("backgroundColor", style, workbook);
    }

    public static XSSFColor getXSSFTopBorderColor(JSONObject style, XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderTopColor", style, workbook);
    }

    public static XSSFColor getXSSFRightBorderColor(JSONObject style,
            XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderRightColor", style, workbook);
    }

    public static XSSFColor getXSSFBottomBorderColor(JSONObject style,
            XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderBottomColor", style, workbook);
    }

    public static XSSFColor getXSSFLeftBorderColor(JSONObject style,
            XSSFWorkbook workbook) {
        return getXSSFColorByHtmlStyleName("borderLeftColor", style, workbook);
    }
    
    private static XSSFColor getXSSFColorByHtmlStyleName(String styleName,
            JSONObject style,
            XSSFWorkbook workbook) {
        if (!style.isNull(styleName)) {
            try {
                if (StringUtils.isNotEmpty(style.getString(styleName))) {
                    short[] rgb = stringRGBToShort(style.getString(styleName));
                    return PoiExcelHelper.getColor(rgb, workbook);
                } else {
                    return PoiExcelHelper.getColor(new short[]{0,0,0}, workbook);
                }
            } catch (JSONException e) {
                return PoiExcelHelper.getColor(new short[]{0,0,0}, workbook);
            }
        }

        return PoiExcelHelper.getColor(new short[]{0,0,0}, workbook);
    }

    public static short getColorByHtmlStyleName(String styleName, JSONObject style, Workbook workbook) {
        if (!style.isNull(styleName)) {
            try {
                return getColorIndex(stringRGBToShort(style.getString(styleName)), workbook);
            } catch (JSONException e) {
                return HSSFColor.HSSFColorPredefined.WHITE.getIndex();
            }
        }

        return HSSFColor.HSSFColorPredefined.WHITE.getIndex();
    }

    public static Font getXSSFFont(JSONObject style, XSSFWorkbook workbook) {
        boolean boldWeight = false;
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
                    boldWeight = true;
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
                    color = PoiExcelHelper.getColor(stringRGBToShort(rgbColor), workbook);
                    indexedColor = color.getIndexed();
                }
            }

            if (!style.isNull("fontSize")) {
                String fontSize = style.getString("fontSize");

                if (fontSize.contains("px")) {
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
        } catch (JSONException ignored) {

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
            font.setBold(boldWeight);
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
