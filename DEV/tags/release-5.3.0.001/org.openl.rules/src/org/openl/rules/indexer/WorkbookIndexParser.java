package org.openl.rules.indexer;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;

/**
 * Parser for workbook. After parsing returns an array of sheets {@link XlsSheetSourceCodeModule},
 * which this workbook contain.
 */
public class WorkbookIndexParser implements IIndexParser {

    public String getCategory() {
        return IDocumentType.WORKBOOK.getCategory();
    }

    public String getType() {
        return IDocumentType.WORKBOOK.getType();
    }
    
    /**
     * After parsing returns an array of sheets {@link IIndexElement},
     * which this workbook contain.
     */
    public IIndexElement[] parse(IIndexElement root) {
        XlsWorkbookSourceCodeModule wbSrc = (XlsWorkbookSourceCodeModule) root;
        return parseWorkbook(wbSrc);
    }
    
    /**
     * After parsing returns an array of sheets {@link XlsSheetSourceCodeModule},
     * which this workbook contain.
     */
    public XlsSheetSourceCodeModule[] parseWorkbook(XlsWorkbookSourceCodeModule wbSrc) {

    	Workbook wb = wbSrc.getWorkbook();
        int nsheets = wb.getNumberOfSheets();

        XlsSheetSourceCodeModule[] sheets = new XlsSheetSourceCodeModule[nsheets];

        for (int i = 0; i < nsheets; i++) {
            Sheet sheet = wb.getSheetAt(i);
            String sheetName = wb.getSheetName(i);

            sheets[i] = new XlsSheetSourceCodeModule(sheet, sheetName, wbSrc);
        }

        return sheets;
    }

}
