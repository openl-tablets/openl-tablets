package org.openl.rules.table.xls;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.table.ui.ICellStyle;
import static org.openl.rules.table.xls.PoiExcelHelper.*;

/**
 * @author snshor
 * @author Andrei Astrouski
 */
public class XlsCellStyle implements ICellStyle {

    private CellStyle xlsStyle;
    private Workbook workbook;

    public XlsCellStyle(CellStyle xlsStyle, Workbook workbook) {
        this.xlsStyle = xlsStyle;
        this.workbook = workbook;
    }

    @Override
    public short[][] getBorderRGB() {
        return getCellBorderColors(xlsStyle, workbook);
    }

    @Override
    public short[] getBorderStyle() {
        return getCellBorderStyles(xlsStyle);
    }

    @Override
    public short getFillPattern() {
        return xlsStyle.getFillPattern();
    }

    public boolean hasNoFill() {
        return (getFillPattern() == CellStyle.NO_FILL);
    }

    @Override
    public short[] getFillBackgroundColor() {
        if (hasNoFill()) {
            return null;
        }

        return toRgb(
                xlsStyle.getFillBackgroundColorColor());
    }

    @Override
    public short getFillBackgroundColorIndex() {
        return xlsStyle.getFillBackgroundColor();
    }

    @Override
    public short[] getFillForegroundColor() {
        if (hasNoFill()) {
            return null;
        }

        return toRgb(
                xlsStyle.getFillForegroundColorColor());
    }

    @Override
    public short getFillForegroundColorIndex() {
        return xlsStyle.getFillForegroundColor();
    }

    @Override
    public int getHorizontalAlignment() {
        return xlsStyle == null ? ALIGN_GENERAL : xlsStyle.getAlignment();
    }

    @Override
    public int getVerticalAlignment() {
        return xlsStyle == null ? ALIGN_GENERAL : xlsStyle.getVerticalAlignment();
    }

    @Override
    public int getIdent() {
        return xlsStyle.getIndention();
    }

    @Override
    public int getRotation() {
        return xlsStyle.getRotation();
    }

    @Override
    public boolean isWrappedText() {
        return xlsStyle.getWrapText();
    }

    public CellStyle getXlsStyle() {
        return xlsStyle;
    }

}