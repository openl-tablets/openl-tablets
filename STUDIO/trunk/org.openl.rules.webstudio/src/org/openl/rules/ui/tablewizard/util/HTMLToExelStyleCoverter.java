package org.openl.rules.ui.tablewizard.util;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.richfaces.json.JSONException;
import org.richfaces.json.JSONObject;

public class HTMLToExelStyleCoverter {
    private final static String TOP = "Top";
    private final static String RIGHT = "Right";
    private final static String BOTTOM = "Bottom";
    private final static String LEFT = "Left";

    JSONObject style;

    static public short getBackgroundColor(JSONObject style) {
        if (!style.isNull("backgroundColor")) {
            try {
                return colorHtmlToExel(style.getString("backgroundColor"));
            } catch (JSONException e) {
                e.printStackTrace();
                return HSSFColor.WHITE.index;
            }
        }

        return HSSFColor.WHITE.index;
    }

    static public short getBorderTop(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.TOP);
    }

    static public short getTopBorderColor(JSONObject style) {
        if (!style.isNull("borderTopColor")) {
            try {
                return colorHtmlToExel(style.getString("borderTopColor"));
            } catch (JSONException e) {
                e.printStackTrace();
                return HSSFColor.BLACK.index;
            }
        }

        return HSSFColor.BLACK.index;
    }

    static public short getBorderRight(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.RIGHT);
    }

    static public short getRightBorderColor(JSONObject style) {
        if (!style.isNull("borderRightColor")) {
            try {
                return colorHtmlToExel(style.getString("borderRightColor"));
            } catch (JSONException e) {
                e.printStackTrace();
                return HSSFColor.BLACK.index;
            }
        }

        return HSSFColor.BLACK.index;
    }

    static public short getBorderBottom(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.BOTTOM);
    }

    static public short getBottomBorderColor(JSONObject style) {
        if (!style.isNull("borderBottomColor")) {
            try {
                return colorHtmlToExel(style.getString("borderBottomColor"));
            } catch (JSONException e) {
                e.printStackTrace();
                return HSSFColor.BLACK.index;
            }
        }

        return HSSFColor.BLACK.index;
    }

    static public short getBorderLeft(JSONObject style) {
        return getBorder(style, HTMLToExelStyleCoverter.LEFT);
    }

    static public short getLeftBorderColor(JSONObject style) {
        if (!style.isNull("borderLeftColor")) {
            try {
                return colorHtmlToExel(style.getString("borderLeftColor"));
            } catch (JSONException e) {
                e.printStackTrace();
                return HSSFColor.BLACK.index;
            }
        }

        return HSSFColor.BLACK.index;
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

                if (!StringUtils.isEmpty(fontFamily)) {
                    name = fontFamily;
                }
            }

            if (!style.isNull("color")) {
                String rgbColor = style.getString("color");

                if(!StringUtils.isEmpty(rgbColor)) {
                    color = colorHtmlToExel(rgbColor);
                }
            }

            if (!style.isNull("fontSize")) {
                String fontSize = style.getString("fontSize");

                if (fontSize.indexOf("px") > -1) {
                    fontSize = fontSize.replace("px", "");
                }

                if (!StringUtils.isEmpty(fontSize)) {
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

    static private short colorHtmlToExel(String rgbColor) {
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

        return getColorIndex(returnRGB);
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

    static private short getColorIndex(short[] rgbColor) {
        if (rgbColor != null) {
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFPalette palette = wb.getCustomPalette();

            HSSFColor hssfColor = palette.findColor((byte) rgbColor[0], (byte) rgbColor[1], (byte) rgbColor[2]);
            if (hssfColor == null ) {
                HSSFColor similarColor = palette.findSimilarColor((int)rgbColor[0], (int)rgbColor[1], (int)rgbColor[2]);
                palette.setColorAtIndex(similarColor.getIndex(), (byte) rgbColor[0], (byte) rgbColor[1], (byte) rgbColor[2]);
                hssfColor = palette.getColor(similarColor.getIndex());
            }

            return hssfColor.getIndex();
        }

        return HSSFColor.WHITE.index;
    }

    static private short getBorder(JSONObject style, String position) {
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
                e.printStackTrace();
                return CellStyle.BORDER_NONE;
            }
        }

        return CellStyle.BORDER_NONE;
    }
}
