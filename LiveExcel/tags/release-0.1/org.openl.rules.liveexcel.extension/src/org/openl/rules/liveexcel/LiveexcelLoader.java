package org.openl.rules.liveexcel;

import java.net.URL;

import org.openl.IOpenSourceCodeModule;
import org.openl.rules.extension.load.IExtensionLoader;
import org.openl.rules.lang.xls.XlsLoader;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.syntax.impl.URLSourceCodeModule;
import org.openl.util.PathTool;

public class LiveexcelLoader implements IExtensionLoader {

    public static final String LIVEEXCEL_TYPE = "liveexcel";
    
    public static final String LIVEEXCEL_MODULE = "liveexcel";

    public String getModuleName() {
        return LIVEEXCEL_MODULE;
    }
    
    public void process(XlsLoader xlsLoader, TableSyntaxNode tsn, IGridTable table, XlsSheetSourceCodeModule sheetSource) {
        int h = table.getLogicalHeight();

        for (int i = 0; i < h; i++) {
            String include = table.getCell(1, i).getStringValue();
            if (include == null) {
                continue;
            }
            include = include.trim();
            if (include.length() == 0) {
                continue;
            }
            String[] split = include.split(";");
            String includePath = split[0];
            String projectName = split.length == 2 ? split[1] : null;
            
            IOpenSourceCodeModule src = null;
            try {
                String newURL = PathTool.mergePath(sheetSource.getWorkbookSource().getUri(0), includePath);
                src = new URLSourceCodeModule(new URL(newURL));
            } catch (Throwable t) {
                ISyntaxError se = new SyntaxError(null, "Include " + includePath + " not found", t,
                        new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));
                xlsLoader.addError(se);
                tsn.addError(se);
                continue;
            }

            try {
                preprocessLiveExcelWorkbook(xlsLoader, src, sheetSource, projectName);
            } catch (Throwable t) {
                ISyntaxError se = new SyntaxError(null, "Include " + include + " not found", t,
                        new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));
                xlsLoader.addError(se);
                tsn.addError(se);
                continue;
            }
        }
    }

    private void preprocessLiveExcelWorkbook(XlsLoader xlsLoader, IOpenSourceCodeModule source,
            XlsSheetSourceCodeModule sheetSource, String projectName) {
        String uri = source.getUri(0);
        if (xlsLoader.getPreprocessedWorkBooks().contains(uri)) {
            return;
        }
        xlsLoader.getPreprocessedWorkBooks().add(uri);
        LiveExcelWorkbook wb;
        try {
            wb = LiveExcelWorkbookFactory.create(source.getByteStream(), projectName);
            XlsWorkbookSourceCodeModule xlsWorkbookSourceCodeModule = new XlsWorkbookSourceCodeModule(source, wb);
            DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(wb);
            searcher.findFunctions();
            xlsLoader.addExtensionNode(new IdentifierNode(LIVEEXCEL_TYPE, null, source.getCode(), xlsWorkbookSourceCodeModule));
        } catch (Exception e) {
             throw new RuntimeException("Error processing file.", e);
        }
    }

}
