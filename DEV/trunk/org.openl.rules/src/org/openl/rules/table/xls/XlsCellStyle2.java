package org.openl.rules.table.xls;

import java.awt.Color;
import java.util.Hashtable;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlObject;
import org.openl.rules.table.ui.ICellStyle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorScheme;
import org.openxmlformats.schemas.drawingml.x2006.main.ThemeDocument;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;

public class XlsCellStyle2 implements ICellStyle {

    @SuppressWarnings("unchecked")
    private static Hashtable<Integer, HSSFColor> oldIndexedColors = HSSFColor.getIndexHash();

    private XSSFCellStyle xlsStyle;
    private XSSFWorkbook workbook;

    /**
     * ThemeDocument that contains theme colors.
     */
    private ThemeDocument themeDocument;

    public XlsCellStyle2(XSSFCellStyle xlsStyle, XSSFWorkbook workbook) {
        this.xlsStyle = xlsStyle;
        this.workbook = workbook;

        init();
    }

    /**
     * Initialize.
     */
    private void init() {

        try {
            // Read xml part of workbook that contains theme description.
            //
            POIXMLDocumentPart themePart = getTheme(workbook);

            if (themePart != null) {
                themeDocument = ThemeDocument.Factory.parse(themePart.getPackagePart().getInputStream());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public short[][] getBorderRGB() {
        short[][] ccRgb = new short[4][];
        ccRgb[0] = colorToArray(xlsStyle.getTopBorderXSSFColor());
        ccRgb[1] = colorToArray(xlsStyle.getRightBorderXSSFColor());
        ccRgb[2] = colorToArray(xlsStyle.getBottomBorderXSSFColor());
        ccRgb[3] = colorToArray(xlsStyle.getLeftBorderXSSFColor());

        return ccRgb;
    }
    
    public static short[] colorToArray(XSSFColor color, XSSFWorkbook workbook){
        return new XlsCellStyle2(null, workbook).colorToArray(color);
    }

    private short[] colorToArray(XSSFColor color) {

        if (color == null) {
            return null;
        }

        byte[] rgb = color.getRgb();

        if (rgb == null) {

            Integer key = new Integer(color.getIndexed());
            HSSFColor c = oldIndexedColors.get(key);

            if (c != null) {
                return c.getTriplet();
            } else {

                // If color not identified yet try to check that given color is
                // theme color.
                // NOTE: the following code used as temporal workaround for
                // getting theme color for cell. Apache POI library not
                // supported themes (3.5 version) at high
                // level of framework API.
                //
                // author: Alexey Gamanovich
                //
                if (themeDocument != null && color.getCTColor() != null && color.getCTColor().isSetTheme()) {

                    CTColor ctColor = color.getCTColor();

                    int themeIndex = (int) ctColor.getTheme();

                    // the value to apply
                    // to RGB to make it
                    // lighter or darker
                    double tint = ctColor.getTint();

                    try {
                        return getThemeColorRgb(themeIndex, tint);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return null;
                }
            }
        }

        short[] result = new short[3];
        for (int i = 1; i < 4; i++) {
            result[i - 1] = (short)(rgb[i] & 0xFF);
        }

        return result;
    }

    public short[] getBorderStyle() {
        short[] bb = new short[4];
        bb[0] = xlsStyle.getBorderTop();
        bb[1] = xlsStyle.getBorderRight();
        bb[2] = xlsStyle.getBorderBottom();
        bb[3] = xlsStyle.getBorderLeft();
        return bb;
    }

    public boolean hasNoFill() {
        return (xlsStyle.getFillPattern() == CellStyle.NO_FILL);
    }

    public short[] getFillBackgroundColor() {
        if (hasNoFill())
            return null;
        return colorToArray(xlsStyle.getFillBackgroundXSSFColor());
    }

    public short[] getFillForegroundColor() {
        if (hasNoFill())
            return null;
        return colorToArray(xlsStyle.getFillForegroundXSSFColor());
    }

    public int getHorizontalAlignment() {
        return xlsStyle == null ? ALIGN_GENERAL : xlsStyle.getAlignment();
    }

    public int getIdent() {
        return xlsStyle.getIndention();
    }

    public int getRotation() {
        return xlsStyle.getRotation();
    }

    public String getTextFormat() {
        return xlsStyle.getDataFormatString();
    }

    public int getVerticalAlignment() {
        return xlsStyle == null ? ALIGN_GENERAL : xlsStyle.getVerticalAlignment();
    }

    public boolean isWrappedText() {
        return xlsStyle.getWrapText();
    }

    public CellStyle getXlsStyle() {
        return xlsStyle;
    }

    /**
     * Returns xml document part that contains theme description.
     * 
     * @param wb {@link XSSFWorkbook} object
     * @return document part with theme
     * @throws Exception if an error has occured
     */
    private POIXMLDocumentPart getTheme(XSSFWorkbook wb) throws Exception {

        for (POIXMLDocumentPart p : wb.getRelations()) {
            if (p.getPackageRelationship().getRelationshipType().equals(XSSFRelation.THEME.getRelation())) {
                return p;
            }
        }

        return null;
    }

    /**
     * Gets theme color as RGB triple.
     * 
     * @param idx index of color in theme
     * @param tint tint that make color lighter or darker
     * @return RGB triple if color in theme found; null - otherwise
     * @throws Exception if an error has occured
     */
    private short[] getThemeColorRgb(int idx, double tint) throws Exception {

        CTColorScheme colorScheme = themeDocument.getTheme().getThemeElements().getClrScheme();
        org.openxmlformats.schemas.drawingml.x2006.main.CTColor ctColor = null;

        int cnt = 0;

        for (XmlObject obj : colorScheme.selectPath("./*")) {

            if (obj instanceof org.openxmlformats.schemas.drawingml.x2006.main.CTColor) {
                if (cnt == idx) {
                    ctColor = (org.openxmlformats.schemas.drawingml.x2006.main.CTColor) obj;
                    break;
                }
                cnt++;
            }
        }

        if (ctColor == null) {
            return null;
        }

        // To get tint of some color we have to convert our color from RGB to
        // HSL, adjust the luminance (L) and convert back to RGB(see
        // http://msdn.microsoft.com/en-us/library/dd560821.aspx)
        float hsl[];
        if (idx > 3) {
            byte[] rgb = ctColor.getSrgbClr().getVal();
            hsl = convertRGBtoHSL(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
        } else {
            byte[] rgb;
            if (ctColor.getSrgbClr() != null) {
                rgb = ctColor.getSrgbClr().getVal();//for tints of brown and blue colors
            } else {
                rgb = ctColor.getSysClr().getLastClr();//for tints of white and black colors
            }
            hsl = convertRGBtoHSL(255 - rgb[0] & 0xFF, 255 - rgb[1] & 0xFF, 255 - rgb[2] & 0xFF);

        }
        float lightness;
        if (tint > 0) {
            lightness = (float) Math.min(1.0F, Math.max(0.0F, hsl[2] * (1 - tint) + tint));
        } else if (tint == 0) {
            lightness = hsl[2];
        } else {
            lightness = (float) Math.min(1.0F, Math.max(0.0F, hsl[2] * (1 + tint)));
        }
        return convertHSLtoRGB(hsl[0], hsl[1], lightness);
    }

    private static float[] convertRGBtoHSL(int r, int g, int b) {

        float var_R = (r / 255f);
        float var_G = (g / 255f);
        float var_B = (b / 255f);

        float var_Min; // Min. value of RGB
        float var_Max; // Max. value of RGB
        float del_Max; // Delta RGB value

        if (var_R > var_G) {
            var_Min = var_G;
            var_Max = var_R;
        } else {
            var_Min = var_R;
            var_Max = var_G;
        }

        if (var_B > var_Max)
            var_Max = var_B;
        if (var_B < var_Min)
            var_Min = var_B;

        del_Max = var_Max - var_Min;

        float H = 0, S, L;
        L = (var_Max + var_Min) / 2f;

        if (del_Max == 0) {
            H = 0;
            S = 0;
        } // gray
        else { // Chroma
            if (L < 0.5)
                S = del_Max / (var_Max + var_Min);
            else
                S = del_Max / (2 - var_Max - var_Min);

            float del_R = (((var_Max - var_R) / 6f) + (del_Max / 2f)) / del_Max;
            float del_G = (((var_Max - var_G) / 6f) + (del_Max / 2f)) / del_Max;
            float del_B = (((var_Max - var_B) / 6f) + (del_Max / 2f)) / del_Max;

            if (var_R == var_Max)
                H = del_B - del_G;
            else if (var_G == var_Max)
                H = (1 / 3f) + del_R - del_B;
            else if (var_B == var_Max)
                H = (2 / 3f) + del_G - del_R;
            if (H < 0)
                H += 1;
            if (H > 1)
                H -= 1;
        }
        return new float[] { H * 360, S, L };
    }

    private static short[] convertHSLtoRGB(float h, float s, float l) {
        float q;
        if ((double) l < 0.5D) {
            q = l * (1.0F + s);
        } else {
            q = (l + s) - l * s;
        }
        float p = 2.0F * l - q;
        float hNorm = h / 360F;
        float tR = hNorm + 0.3333333F;
        float tG = hNorm;
        float tB = hNorm - 0.3333333F;
        float r = convertHueToRGB(tR, p, q);
        float g = convertHueToRGB(tG, p, q);
        float b = convertHueToRGB(tB, p, q);
        Color rgbColor = new Color(r, g, b);
        return new short[] { (short) rgbColor.getRed(), (short) rgbColor.getGreen(), (short) rgbColor.getBlue() };
    }

    private static float convertHueToRGB(float tC, float p, float q) {
        if (tC < 0.0F) {
            tC++;
        }
        if (tC > 1.0F) {
            tC--;
        }
        float retVal;
        if (6F * tC < 1.0F) {
            retVal = p + (q - p) * 6F * tC;
        } else if (2.0F * tC < 1.0F) {
            retVal = q;
        } else if (3F * tC < 2.0F) {
            retVal = p + (q - p) * 6F * (0.6666667F - tC);
        } else {
            retVal = p;
        }
        return retVal;
    }
}
