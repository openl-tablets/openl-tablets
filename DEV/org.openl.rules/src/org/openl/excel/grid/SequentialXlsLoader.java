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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequentialXlsLoader extends XlsLoader {
    private final Logger log = LoggerFactory.getLogger(SequentialXlsLoader.class);

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
        String path = getPath(workbookSourceModule);
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

    /**
     * Get path on file system for xls/xlsx file. Returns null if path file isn't on file system (for example inside of
     * jar). Example of such case is AlgorithmTableSpecification.xls.
     *
     * @param workbookSourceModule module to get the path
     * @return path on file system or null if path can't be retrieved.
     */
    private String getPath(XlsWorkbookSourceCodeModule workbookSourceModule) {
        String uri = workbookSourceModule.getSource().getUri();
        log.debug("Workbook uri: {}", uri);
        String path = null;
        if (!uri.startsWith("jar:") && !uri.startsWith("vfs:")) {
            path = workbookSourceModule.getSourceFile().getAbsolutePath();
        }
        return path;
    }

}
