package org.openl.rules.ui.tablewizard.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.table.xls.PoiExcelHelper;
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
                CellStyle cellStyle = PoiExcelHelper.createCellStyle(workbook);

                cellStyle.setFillForegroundColor((HTMLToExelStyleCoverter.getBackgroundColor(style, workbook)));
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

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
                XSSFCellStyle cellStyle = PoiExcelHelper.createCellStyle(workbook);

                cellStyle.setFillForegroundColor((HTMLToExelStyleCoverter.getXSSFBackgroundColor(style)));
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                cellStyle.setTopBorderColor(HTMLToExelStyleCoverter.getXSSFTopBorderColor(style));
                cellStyle.setRightBorderColor(HTMLToExelStyleCoverter.getXSSFRightBorderColor(style));
                cellStyle.setBottomBorderColor(HTMLToExelStyleCoverter.getXSSFBottomBorderColor(style));
                cellStyle.setLeftBorderColor(HTMLToExelStyleCoverter.getXSSFLeftBorderColor(style));
                
                cellStyle.setBorderTop(HTMLToExelStyleCoverter.getBorderTop(style));
                cellStyle.setBorderRight(HTMLToExelStyleCoverter.getBorderRight(style));
                cellStyle.setBorderBottom(HTMLToExelStyleCoverter.getBorderBottom(style));
                cellStyle.setBorderLeft(HTMLToExelStyleCoverter.getBorderLeft(style));
                cellStyle.setAlignment(HTMLToExelStyleCoverter.getAlignment(style));
                cellStyle.setFont(HTMLToExelStyleCoverter.getXSSFFont(style, (XSSFWorkbook) workbook));

                return new XlsCellStyle(cellStyle, workbook);
            }

            return new XlsCellStyle(PoiExcelHelper.createCellStyle(workbook), workbook);
        } else {
            Workbook workbook = gridModel.getSheetSource()
                    .getWorkbookSource().getWorkbook();
            CellStyle cellStyle = PoiExcelHelper.createCellStyle(workbook);

            return new XlsCellStyle(cellStyle, workbook);
        }
    }
}
