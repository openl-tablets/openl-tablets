package org.openl.rules.table.xls;

import static org.openl.rules.table.xls.PoiExcelHelper.*;

import org.apache.poi.ss.usermodel.*;
import org.openl.rules.table.ui.ICellStyle;

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
    public BorderStyle[] getBorderStyle() {
        return getCellBorderStyles(xlsStyle);
    }

    @Override
    public FillPatternType getFillPattern() {
        return xlsStyle.getFillPattern();
    }

    public boolean hasNoFill() {
        return getFillPattern() == FillPatternType.NO_FILL;
    }

    @Override
    public short[] getFillBackgroundColor() {
        if (hasNoFill()) {
            return null;
        }

        return toRgb(xlsStyle.getFillBackgroundColorColor());
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

        return toRgb(xlsStyle.getFillForegroundColorColor());
    }

    @Override
    public short getFillForegroundColorIndex() {
        return xlsStyle.getFillForegroundColor();
    }

    @Override
    public HorizontalAlignment getHorizontalAlignment() {
        return xlsStyle == null ? HorizontalAlignment.GENERAL : xlsStyle.getAlignment();
    }

    @Override
    public VerticalAlignment getVerticalAlignment() {
        return xlsStyle == null ? VerticalAlignment.TOP : xlsStyle.getVerticalAlignment();
    }

    @Override
    public int getIndent() {
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

    @Override
    public short getFormatIndex() {
        return xlsStyle.getDataFormat();
    }

    @Override
    public String getFormatString() {
        return xlsStyle.getDataFormatString();
    }

    public CellStyle getXlsStyle() {
        return xlsStyle;
    }

}