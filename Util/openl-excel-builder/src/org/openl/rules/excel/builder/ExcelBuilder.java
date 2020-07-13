package org.openl.rules.excel.builder;

import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.excel.builder.export.XlsExtendedSheetGridModel;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.source.impl.URLSourceCodeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ExcelBuilder.class);

    public static XlsExtendedSheetGridModel createGrid(Sheet sheet, String fileName) {
        final SimpleWorkbookLoader workbookLoader = new SimpleWorkbookLoader(sheet.getWorkbook());
        XlsWorkbookSourceCodeModule mockWorkbookSource = new XlsWorkbookSourceCodeModule(
            new URLSourceCodeModule(fileName),
            workbookLoader);
        XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet),
            mockWorkbookSource);

        return new XlsExtendedSheetGridModel(mockSheetSource);
    }

}
