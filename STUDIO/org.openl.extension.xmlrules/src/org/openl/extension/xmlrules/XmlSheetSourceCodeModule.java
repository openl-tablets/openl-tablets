package org.openl.extension.xmlrules;

import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;

public class XmlSheetSourceCodeModule extends XlsSheetSourceCodeModule {
    private final LazyWorkbook xmlWorkbook;
    private final int sheetIndex;

    public XmlSheetSourceCodeModule(int sheetIndex, XlsWorkbookSourceCodeModule workbookSource, LazyWorkbook xmlWorkbook) {
        super(sheetIndex, workbookSource);
        this.sheetIndex = sheetIndex;
        this.xmlWorkbook = xmlWorkbook;
    }

    @Override
    public String getSheetName() {
        // Sheet name is used as category name in WebStudio
        return xmlWorkbook.getXlsFileName() + "#" + xmlWorkbook.getSheets().get(sheetIndex).getName();
    }
}
