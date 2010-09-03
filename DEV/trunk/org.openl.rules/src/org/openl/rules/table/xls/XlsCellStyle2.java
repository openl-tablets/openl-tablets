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
            // TODO FIXME (byte: -128 .. 127), short-color: 0..255 or 0..65xxx ?
            result[i - 1] = rgb[i];
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
        if (idx > 2) {
            byte[] rgb = ctColor.getSrgbClr().getVal();
            hsl = convertRGBtoHSL(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
        } else {
            byte[] rgb;
            if (ctColor.getSrgbClr() != null) {
                rgb = ctColor.getSrgbClr().getVal();//for brown color tints
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
        float varR = (float) r / 255F;
        float varG = (float) g / 255F;
        float varB = (float) b / 255F;
        float varMin = Math.min(varR, Math.min(varG, varB));
        float varMax = Math.max(varR, Math.max(varG, varB));
        float delMax = varMax - varMin;
        float h = 0.0F;
        float s = 0.0F;
        float l = (varMax + varMin) / 2.0F;
        if (delMax == 0.0F || l == 0.0F) {
            s = 0.0F;
        } else if (l == 1.0F) {
            s = 1.0F;
        } else if ((double) l <= 0.5D) {
            s = delMax / (2.0F * (1.0F - l));
        } else if ((double) l > 0.5D) {
            s = delMax / (2.0F * l);
        }
        if (delMax == 0.0F) {
            h = 0.0F;
        } else if (varMax == varR && g >= b) {
            h = (60F * (varG - varB)) / delMax + 0.0F;
        } else if (varMax == varR && varG < (float) b) {
            h = (60F * (varG - varB)) / delMax + 360F;
        } else if (varMax == varG) {
            h = (60F * (varB - varR)) / delMax + 120F;
        } else if (varMax == varB) {
            h = (60F * (varR - varG)) / delMax + 240F;
        }
        return new float[] { h, s, l };
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
