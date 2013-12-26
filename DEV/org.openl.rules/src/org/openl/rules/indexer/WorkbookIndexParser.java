package org.openl.rules.indexer;

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

        int nsheets = wbSrc.getWorkbookLoader().getNumberOfSheets();

        XlsSheetSourceCodeModule[] sheets = new XlsSheetSourceCodeModule[nsheets];

        for (int i = 0; i < nsheets; i++) {
            sheets[i] = new XlsSheetSourceCodeModule(i, wbSrc);
        }

        return sheets;
    }

}
