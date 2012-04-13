package org.openl.rules.liveexcel;

import java.io.File;

import org.openl.rules.extension.load.IExtensionLoader;
import org.openl.rules.lang.xls.XlsLoader;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.util.PathTool;

import com.exigen.le.LiveExcel;
import com.exigen.le.smodel.provider.ServiceModelJAXB;
import com.exigen.le.usermodel.LiveExcelWorkbook;
import com.exigen.le.usermodel.LiveExcelWorkbookFactory;

public class LiveexcelLoader implements IExtensionLoader {

    public static final String LIVEEXCEL_TYPE = "liveexcel";

    public static final String LIVEEXCEL_MODULE = "liveexcel";

    public String getModuleName() {
        return LIVEEXCEL_MODULE;
    }

    public void process(XlsLoader xlsLoader, TableSyntaxNode tsn, IGridTable table, XlsSheetSourceCodeModule sheetSource) {
        ILogicalTable logicalTable = LogicalTableHelper.logicalTable(table);
        int h = logicalTable.getHeight();

        for (int i = 0; i < h; i++) {
            IGridTable includeCell = logicalTable.getSubtable(1, i, 1, 1).getSource();
            String include = includeCell.getCell(0, 0).getStringValue();
            if (include == null) {
                continue;
            }
            include = include.trim();
            if (include.length() == 0) {
                continue;
            }
            String[] split = include.split(";");
            String includePath = split[0];

            FileSourceCodeModule src = null;
            try {
                String newURI = PathTool.mergePath(sheetSource.getWorkbookSource().getUri(0), includePath);
                src = new FileSourceCodeModule(new File(sheetSource.getWorkbookSource().getSourceFile().getParent(), includePath), newURI);
            } catch (Throwable t) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Include " + includePath
                        + " not found", t, null, new GridCellSourceCodeModule(includeCell));
                xlsLoader.addError(error);
                tsn.addError(error);
                continue;
            }

            try {
                preprocessLiveExcelWorkbook(xlsLoader, src, sheetSource);
            } catch (Throwable t) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                        "Failed to process LiveExcel include: " + include + ". Reason: " + t.getMessage(), t, null,
                        new GridCellSourceCodeModule(includeCell));
                xlsLoader.addError(error);
                tsn.addError(error);
                continue;
            }
        }
    }

    private void preprocessLiveExcelWorkbook(XlsLoader xlsLoader, FileSourceCodeModule source,
            XlsSheetSourceCodeModule sheetSource) throws Exception {
        String uri = source.getUri(0);
        if (xlsLoader.getPreprocessedWorkBooks().contains(uri)) {
            return;
        }
        xlsLoader.getPreprocessedWorkBooks().add(uri);
        LiveExcelWorkbook wb;
        File leProjectElementsFolder = source.getFile().getParentFile();
        LiveExcel liveExcel = new LiveExcel(new ServiceModelJAXB(leProjectElementsFolder));
        wb = LiveExcelWorkbookFactory.create(source.getByteStream());
        XlsWorkbookSourceCodeModule xlsWorkbookSourceCodeModule = new XlsWorkbookSourceCodeModule(source, wb);
        xlsLoader.addExtensionNode(new LiveExcelIdentifierNode(LIVEEXCEL_TYPE, null, source.getCode(),
                xlsWorkbookSourceCodeModule, liveExcel));
    }

}
