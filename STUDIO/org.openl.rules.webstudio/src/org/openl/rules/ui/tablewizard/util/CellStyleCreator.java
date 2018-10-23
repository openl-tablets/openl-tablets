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

                cellStyle.setFillForegroundColor((HTMLToExcelStyleCoverter.getBackgroundColor(style, workbook)));
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                cellStyle.setTopBorderColor(HTMLToExcelStyleCoverter.getTopBorderColor(style, workbook));
                cellStyle.setRightBorderColor(HTMLToExcelStyleCoverter.getRightBorderColor(style, workbook));
                cellStyle.setBottomBorderColor(HTMLToExcelStyleCoverter.getBottomBorderColor(style, workbook));
                cellStyle.setLeftBorderColor(HTMLToExcelStyleCoverter.getLeftBorderColor(style, workbook));

                cellStyle.setBorderTop(HTMLToExcelStyleCoverter.getBorderTop(style));
                cellStyle.setBorderRight(HTMLToExcelStyleCoverter.getBorderRight(style));
                cellStyle.setBorderBottom(HTMLToExcelStyleCoverter.getBorderBottom(style));
                cellStyle.setBorderLeft(HTMLToExcelStyleCoverter.getBorderLeft(style));
                cellStyle.setAlignment(HTMLToExcelStyleCoverter.getAlignment(style));
                cellStyle.setFont(HTMLToExcelStyleCoverter.getFont(style, workbook));
 
                return new XlsCellStyle(cellStyle, workbook);
            } else if (workbook instanceof XSSFWorkbook) {
                XSSFWorkbook xssfWorkbook = (XSSFWorkbook) workbook;
                XSSFCellStyle cellStyle = PoiExcelHelper.createCellStyle(workbook);

                cellStyle.setFillForegroundColor((HTMLToExcelStyleCoverter.getXSSFBackgroundColor(style, xssfWorkbook)));
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                cellStyle.setTopBorderColor(HTMLToExcelStyleCoverter.getXSSFTopBorderColor(style, xssfWorkbook));
                cellStyle.setRightBorderColor(HTMLToExcelStyleCoverter.getXSSFRightBorderColor(style, xssfWorkbook));
                cellStyle.setBottomBorderColor(HTMLToExcelStyleCoverter.getXSSFBottomBorderColor(style, xssfWorkbook));
                cellStyle.setLeftBorderColor(HTMLToExcelStyleCoverter.getXSSFLeftBorderColor(style, xssfWorkbook));
                
                cellStyle.setBorderTop(HTMLToExcelStyleCoverter.getBorderTop(style));
                cellStyle.setBorderRight(HTMLToExcelStyleCoverter.getBorderRight(style));
                cellStyle.setBorderBottom(HTMLToExcelStyleCoverter.getBorderBottom(style));
                cellStyle.setBorderLeft(HTMLToExcelStyleCoverter.getBorderLeft(style));
                cellStyle.setAlignment(HTMLToExcelStyleCoverter.getAlignment(style));
                cellStyle.setFont(HTMLToExcelStyleCoverter.getXSSFFont(style, xssfWorkbook));

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
