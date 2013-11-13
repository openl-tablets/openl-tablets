package org.openl.rules.ui.tablewizard.util;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.table.xls.XlsCellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.richfaces.json.JSONObject;

public class CellStyleCreator {
    private XlsSheetGridModel gridModel;

    public CellStyleCreator(XlsSheetGridModel gridModel) {
        this.gridModel = gridModel;
    }

    public XlsCellStyle getCellStyle(JSONObject style) {
        if (style != null) {
            Workbook workbook = gridModel.getSheetSource()
                    .getWorkbookSource().getWorkbook();
            
            if (workbook instanceof HSSFWorkbook) {
                HSSFCellStyle cellStyle = ((HSSFWorkbook) workbook).createCellStyle();

                cellStyle.setFillForegroundColor((HTMLToExelStyleCoverter.getBackgroundColor(style, workbook)));
                cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

                cellStyle.setTopBorderColor(HTMLToExelStyleCoverter.getTopBorderColor(style, workbook));
                cellStyle.setRightBorderColor(HTMLToExelStyleCoverter.getRightBorderColor(style, workbook));
                cellStyle.setBottomBorderColor(HTMLToExelStyleCoverter.getBottomBorderColor(style, workbook));
                cellStyle.setLeftBorderColor(HTMLToExelStyleCoverter.getLeftBorderColor(style, workbook));

                cellStyle.setBorderTop(HTMLToExelStyleCoverter.getBorderTop(style));
                cellStyle.setBorderRight(HTMLToExelStyleCoverter.getBorderRight(style));
                cellStyle.setBorderBottom(HTMLToExelStyleCoverter.getBorderBottom(style));
                cellStyle.setBorderLeft(HTMLToExelStyleCoverter.getBorderLeft(style));
                cellStyle.setAlignment(HTMLToExelStyleCoverter.getAlignment(style));
                cellStyle.setFont(HTMLToExelStyleCoverter.getFont(style, workbook));
 
                return new XlsCellStyle(cellStyle, workbook);
            } else if (workbook instanceof XSSFWorkbook) {
                XSSFCellStyle cellStyle = ((XSSFWorkbook) workbook).createCellStyle();

                cellStyle.setFillForegroundColor((HTMLToExelStyleCoverter.getXSSFBackgroundColor(style, (XSSFWorkbook)workbook)));
                cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);

                cellStyle.setTopBorderColor(HTMLToExelStyleCoverter.getXSSFTopBorderColor(style, (XSSFWorkbook)workbook));
                cellStyle.setRightBorderColor(HTMLToExelStyleCoverter.getXSSFRightBorderColor(style, (XSSFWorkbook)workbook));
                cellStyle.setBottomBorderColor(HTMLToExelStyleCoverter.getXSSFBottomBorderColor(style, (XSSFWorkbook)workbook));
                cellStyle.setLeftBorderColor(HTMLToExelStyleCoverter.getXSSFLeftBorderColor(style, (XSSFWorkbook)workbook));
                
                cellStyle.setBorderTop(HTMLToExelStyleCoverter.getBorderTop(style));
                cellStyle.setBorderRight(HTMLToExelStyleCoverter.getBorderRight(style));
                cellStyle.setBorderBottom(HTMLToExelStyleCoverter.getBorderBottom(style));
                cellStyle.setBorderLeft(HTMLToExelStyleCoverter.getBorderLeft(style));
                cellStyle.setAlignment(HTMLToExelStyleCoverter.getAlignment(style));
                cellStyle.setFont(HTMLToExelStyleCoverter.getXSSFFont(style, (XSSFWorkbook) workbook));

                return new XlsCellStyle(cellStyle, workbook);
            }

            return new XlsCellStyle(workbook.createCellStyle(), workbook);
        } else {
            Workbook workbook = gridModel.getSheetSource()
                    .getWorkbookSource().getWorkbook();
            CellStyle cellStyle = workbook.createCellStyle();

            return new XlsCellStyle(cellStyle, workbook);
        }
    }
}
