package org.openl.excel.grid;

import org.apache.poi.ss.usermodel.Sheet;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to prevent full xls load using User API (DOM approach). Base class loads the file fully when gets
 * sheet name (it's needed when create Categories in WebStudio and when building sheet URI). To prevent it we get sheet
 * name from SheetDescriptor.
 */
class SequentialXlsSheetSourceCodeModule extends XlsSheetSourceCodeModule {
    private final Logger log = LoggerFactory.getLogger(SequentialXlsSheetSourceCodeModule.class);
    private final SheetDescriptor sheet;

    public SequentialXlsSheetSourceCodeModule(XlsWorkbookSourceCodeModule module, SheetDescriptor sheet) {
        super(sheet.getIndex(), module);
        this.sheet = sheet;
    }

    @Override
    public String getSheetName() {
        // Sheet name is used as category name in WebStudio and in URI
        return sheet.getName();
    }

    @Override
    public Sheet getSheet() {
        log.debug("Full sheet load. Should be invoked only when edit any table.");
        return super.getSheet();
    }
}
