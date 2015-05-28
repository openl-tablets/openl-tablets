package org.openl.rules.table.xls;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.source.impl.FileSourceCodeModule;

public class XlsSheetGridHelper {
    
    private static String VIRTUAL_EXCEL_FILE = "/VIRTUAL_EXCEL_FILE.xls"; 
    
    private XlsSheetGridHelper() {
        
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
                new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(virtualExcelFile, null), new SimpleWorkbookLoader(sheet.getWorkbook()));
        XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet), mockWorkbookSource);
        
        return new XlsSheetGridModel(mockSheetSource);
    }
    
    private static String getDefaultFileName(Sheet sheet) {
        return (sheet.getSheetName() + VIRTUAL_EXCEL_FILE).intern();
    }

}
