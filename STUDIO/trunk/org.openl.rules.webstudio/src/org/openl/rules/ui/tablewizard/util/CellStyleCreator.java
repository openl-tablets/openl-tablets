package org.openl.rules.ui.tablewizard.util;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

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
            CellStyle cellStyle = workbook.createCellStyle();


            //cellStyle.setFillBackgroundColor(HTMLToExelStyleCoverter.getBackgroundColor(style));
            cellStyle.setFillForegroundColor((HTMLToExelStyleCoverter.getBackgroundColor(style)));
            cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

            cellStyle.setBorderTop(HTMLToExelStyleCoverter.getBorderTop(style));
            cellStyle.setTopBorderColor(HTMLToExelStyleCoverter.getTopBorderColor(style));

            cellStyle.setBorderRight(HTMLToExelStyleCoverter.getBorderRight(style));
            cellStyle.setRightBorderColor(HTMLToExelStyleCoverter.getRightBorderColor(style));

            cellStyle.setBorderBottom(HTMLToExelStyleCoverter.getBorderBottom(style));
            cellStyle.setBottomBorderColor(HTMLToExelStyleCoverter.getBottomBorderColor(style));

            cellStyle.setBorderLeft(HTMLToExelStyleCoverter.getBorderLeft(style));
            cellStyle.setLeftBorderColor(HTMLToExelStyleCoverter.getLeftBorderColor(style));

            cellStyle.setAlignment(HTMLToExelStyleCoverter.getAlignment(style));

            cellStyle.setFont(HTMLToExelStyleCoverter.getFont(style, workbook));

            return new XlsCellStyle(cellStyle, workbook);
        } else {
            Workbook workbook = gridModel.getSheetSource()
                    .getWorkbookSource().getWorkbook();
            CellStyle cellStyle = workbook.createCellStyle();

            return new XlsCellStyle(cellStyle, workbook);
        }
    }
}
