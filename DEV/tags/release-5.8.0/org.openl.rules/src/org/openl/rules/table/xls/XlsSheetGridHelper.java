package org.openl.rules.table.xls;

import static org.openl.rules.table.xls.XlsSheetGridExporter.SHEET_NAME;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.util.export.IExporter;

public class XlsSheetGridHelper {
    
    private static String VIRTUAL_EXCEL_FILE = "/VIRTUAL_EXCEL_FILE.xls"; 
    
    private XlsSheetGridHelper() {
        
    }
    
    public static IExporter createExporter(XlsWorkbookSourceCodeModule workbookModule) {
        Workbook workbook = workbookModule.getWorkbook();
        Sheet sheet;
        synchronized (workbook) {
            sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.createSheet(SHEET_NAME);
            }
        }

        return new XlsSheetGridExporter(workbook, createVirtualGrid(sheet));
    }
    
    /**
     * Creates virtual {@link XlsSheetGridModel} from poi source sheet.
     * 
     * @param sheet poi sheet source
     * @return 
     */
    public static XlsSheetGridModel createVirtualGrid(Sheet sheet) {
        String virtualExcelFile = getDefaultFileName(sheet);
        return createVirtualGrid(sheet, virtualExcelFile);
    }    
    
    /**
     * Creates virtual {@link XlsSheetGridModel} from poi source sheet.
     * 
     * @param sheet poi sheet source
     * @param virtualExcelFile file name, if null or blank will be used default name.
     * @return
     */
    public static XlsSheetGridModel createVirtualGrid(Sheet sheet, String virtualExcelFile) {
        if (StringUtils.isBlank(virtualExcelFile)) {
            virtualExcelFile = getDefaultFileName(sheet);
        }
        XlsWorkbookSourceCodeModule mockWorkbookSource = 
            new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(virtualExcelFile, null), sheet.getWorkbook());
        XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(sheet, sheet.getSheetName(), mockWorkbookSource);
        
        return new XlsSheetGridModel(mockSheetSource);
    }
    
    private static String getDefaultFileName(Sheet sheet) {
        return String.format("%s%s", sheet.getSheetName(), VIRTUAL_EXCEL_FILE);
    }

}
