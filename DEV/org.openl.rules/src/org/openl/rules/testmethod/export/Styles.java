package org.openl.rules.testmethod.export;

import static org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.formatters.FormatConstants;

final class Styles {
    static final int HEADER = 0xBFBFBF;
    static final int GREEN_MAIN = 0xC4D79B;
    static final int RED_MAIN = 0xDA9694;
    static final int GREEN_FIELDS = 0xD8E4BC;
    static final int RED_FIELDS = 0xE6B8B7;
    static final int ABSENT_VALUE = 0xEFEFEF;

    final CellStyle testNameSuccess;
    final CellStyle testNameFailure;
    final CellStyle testInfo;

    final CellStyle header;

    final CellStyle resultSuccessId;
    final CellStyle resultFailureId;
    final CellStyle resultSuccessStatus;
    final CellStyle resultFailureStatus;
    final CellStyle resultSuccess;
    final CellStyle resultFailure;
    final CellStyle resultOther;

    final CellStyle parametersInfo;
    final CellStyle parameterValue;
    final CellStyle parameterAbsent;

    private final Map<CellStyle, CellStyle> dateStyles = new HashMap<>();

    Styles(SXSSFWorkbook wb) {
        testNameSuccess = textStyle(wb, createFont(wb, GREEN.getIndex()));
        testNameFailure = textStyle(wb, createFont(wb, RED.getIndex()));
        testInfo = textStyle(wb, createFont(wb, GREY_50_PERCENT.getIndex()));

        header = backgroundStyle(wb, HEADER);
        resultSuccessId = backgroundStyle(wb, GREEN_MAIN);
        resultFailureId = backgroundStyle(wb, RED_MAIN);
        resultSuccessStatus = backgroundStyle(wb, GREEN_MAIN, createFont(wb, GREEN.getIndex()));
        resultFailureStatus = backgroundStyle(wb, RED_MAIN, createFont(wb, RED.getIndex()));
        resultSuccess = backgroundStyle(wb, GREEN_FIELDS);
        resultFailure = backgroundStyle(wb, RED_FIELDS);
        resultOther = backgroundStyle(wb, null);

        parametersInfo = textStyle(wb, createFont(wb, BLACK.getIndex()));
        parameterValue = backgroundStyle(wb, null);
        parameterAbsent = backgroundStyle(wb, ABSENT_VALUE);
    }

    CellStyle getDateStyle(Workbook workbook, CellStyle original) {
        CellStyle dateStyle = dateStyles.get(original);

        if (dateStyle == null) {
            dateStyle = PoiExcelHelper.createCellStyle(workbook);
            dateStyle.cloneStyleFrom(original);
            dateStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));
            dateStyles.put(original, dateStyle);
        }

        return dateStyle;
    }

    private CellStyle textStyle(Workbook workbook, Font font) {
        CellStyle style = workbook.createCellStyle();
        if (font != null) {
            style.setFont(font);
        }
        return style;
    }

    private CellStyle backgroundStyle(SXSSFWorkbook workbook, Integer rgb) {
        return backgroundStyle(workbook, rgb, null);
    }

    private CellStyle backgroundStyle(SXSSFWorkbook workbook, Integer rgb, Font font) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();

        if (rgb != null) {
            IndexedColorMap indexedColors = workbook.getXSSFWorkbook().getStylesSource().getIndexedColors();
            XSSFColor color = new XSSFColor(convertRGB(rgb), indexedColors);
            style.setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        if (font != null) {
            style.setFont(font);
        }

        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private Font createFont(Workbook workbook, short color) {
        Font font = workbook.createFont();
        font.setColor(color);
        font.setBold(true);
        return font;
    }

    static byte[] convertRGB(int rgb) {
        byte red = (byte) (rgb >> 16 & 0xFF);
        byte green = (byte) (rgb >> 8 & 0xFF);
        byte blue = (byte) (rgb & 0xFF);
        return new byte[] { red, green, blue };
    }

}
