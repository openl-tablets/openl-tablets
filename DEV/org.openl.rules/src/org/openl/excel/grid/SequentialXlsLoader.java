package org.openl.excel.grid;

import java.util.List;

import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.ExcelReaderFactory;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.rules.lang.xls.*;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;

public class SequentialXlsLoader extends XlsLoader {
    public SequentialXlsLoader(IncludeSearcher includeSeeker) {
        super(includeSeeker);
    }

    @Override
    protected WorksheetSyntaxNode[] createWorksheetNodes(TablePartProcessor tablePartProcessor,
            XlsWorkbookSourceCodeModule workbookSourceModule) {
        IOpenSourceCodeModule source = workbookSourceModule.getSource();

        if (VirtualSourceCodeModule.SOURCE_URI.equals(source.getUri())) {
            return super.createWorksheetNodes(tablePartProcessor, workbookSourceModule);
        }

        ExcelReaderFactory factory = ExcelReaderFactory.sequentialFactory();

        // Opening the file by path is preferred because using an InputStream has a higher memory footprint than using a
        // File.
        // See POI documentation. For both: User API and SAX/Event API.
        String path;
        try {
            path = workbookSourceModule.getSourceFile().getAbsolutePath();
        } catch (Exception ex) {
            // No path found to the resource (file) on the native file system.
            // The resource can be inside jar, zip, wsjar, vfs or other virtual file system.
            // Example of such case is AlgorithmTableSpecification.xls.
            path = null;
        }
        try (ExcelReader excelReader = path == null ? factory.create(source.getByteStream()) : factory.create(path)) {
            List<? extends SheetDescriptor> sheets = excelReader.getSheets();
            boolean use1904Windowing = excelReader.isUse1904Windowing();

            int nsheets = sheets.size();
            WorksheetSyntaxNode[] sheetNodes = new WorksheetSyntaxNode[nsheets];

            for (int i = 0; i < nsheets; i++) {
                final SheetDescriptor sheet = sheets.get(i);
                XlsSheetSourceCodeModule sheetSource = new SequentialXlsSheetSourceCodeModule(workbookSourceModule,
                    sheet);
                Object[][] cells = excelReader.getCells(sheet);
                IGridTable[] tables = new ParsedGrid(path, sheetSource, sheet, cells, use1904Windowing).getTables();
                sheetNodes[i] = createWorksheetSyntaxNode(tablePartProcessor, sheetSource, tables);
            }

            return sheetNodes;
        }
    }
}
