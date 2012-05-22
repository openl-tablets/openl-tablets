package org.openl.rules.table.xls;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.table.ui.ICellStyle;

/**
 * 
 * TODO: rename it, and write javaDocs.
 *
 */
public class XlsCellStyle2 implements ICellStyle {

    private XSSFCellStyle xlsStyle;
    private XSSFWorkbook workbook;

    /**
     * ThemeDocument that contains theme colors.
     */

    public XlsCellStyle2(XSSFCellStyle xlsStyle, XSSFWorkbook workbook) {
        this.xlsStyle = xlsStyle;
        this.workbook = workbook;
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

        byte[] rgb = color.getRgbWithTint();

        // Byte to short
        if (rgb != null) {
            short[] result = new short[3];
            for (int i = 0; i < 3; i++) {
                result[i] = (short)(rgb[i] & 0xFF);
            }
            return result;
        }

        return null;
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

    public short getFillPattern() {
        return xlsStyle.getFillPattern();
    }

    public short[] getFillBackgroundColor() {
        if (hasNoFill())
            return null;
        XSSFColor color = xlsStyle.getFillBackgroundXSSFColor();
        return colorToArray(color);
    }

    public short[] getFillForegroundColor() {
        if (hasNoFill())
            return null;
        XSSFColor color = xlsStyle.getFillForegroundXSSFColor();
        return colorToArray(color);
    }

    public short getFillBackgroundColorIndex() {
        return xlsStyle.getFillBackgroundColor();
    }

    public short getFillForegroundColorIndex() {
        return xlsStyle.getFillForegroundColor();
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

    public int getVerticalAlignment() {
        return xlsStyle == null ? ALIGN_GENERAL : xlsStyle.getVerticalAlignment();
    }

    public boolean isWrappedText() {
        return xlsStyle.getWrapText();
    }

    public CellStyle getXlsStyle() {
        return xlsStyle;
    }

}
