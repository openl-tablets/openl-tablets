package org.openl.excel.grid;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;

import org.openl.excel.parser.SheetDescriptor;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;

/**
 * This class is used to prevent full xls load using User API (DOM approach). Base class loads the file fully when gets
 * sheet name (it's needed when create Categories in OpenL Studio and when building sheet URI). To prevent it we get sheet
 * name from SheetDescriptor.
 */
@Slf4j
class SequentialXlsSheetSourceCodeModule extends XlsSheetSourceCodeModule {
    private final SheetDescriptor sheet;

    SequentialXlsSheetSourceCodeModule(XlsWorkbookSourceCodeModule module, SheetDescriptor sheet) {
        super(sheet.getIndex(), module);
        this.sheet = sheet;
    }

    @Override
    public String getSheetName() {
        // Sheet name is used as category name in OpenL Studio and in URI
        return sheet.getName();
    }

    @Override
    public Sheet getSheet() {
        log.debug("Full sheet load. Should be invoked only when edit any table.");
        return super.getSheet();
    }
}
