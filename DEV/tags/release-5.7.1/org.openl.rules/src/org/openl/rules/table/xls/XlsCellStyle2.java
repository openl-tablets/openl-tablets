package org.openl.rules.table.xls;

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

        byte[] rgb = ctColor.getSrgbClr().getVal();

        short r = applyTint(rgb[0] & 0xFF, tint);
        short g = applyTint(rgb[1] & 0xFF, tint);
        short b = applyTint(rgb[2] & 0xFF, tint);

        return new short[] { r, g, b };
    }

    /**
     * Apply tint to color.
     * 
     * @param lum color
     * @param tint tint
     * @return color with applied tint
     */
    private short applyTint(int lum, double tint) {

        // downcast int primitive to short because lum values in range [0..255]
        if (tint > 0) {
            return (short) (lum * (1.0 - tint) + (255 - 255 * (1.0 - tint)));
        } else if (tint < 0) {
            return (short) (lum * (1 + tint));
        } else {
            return (short) lum;
        }
    }
}
