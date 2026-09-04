package org.openl.excel.grid;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.openl.dependency.DependencyType;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.ExcelReaderFactory;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.TablePart;
import org.openl.rules.lang.xls.TablePartProcessor;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.ParserUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;

@Slf4j
public class SequentialXlsLoader {
    private final Collection<String> imports = new HashSet<>();
    private final List<SyntaxNodeException> errors = new ArrayList<>();
    private final Collection<OpenLMessage> messages = new LinkedHashSet<>();
    private final Set<String> preprocessedWorkBooks = new HashSet<>();
    private final List<WorkbookSyntaxNode> workbookNodes = new ArrayList<>();
    private final Set<IDependency> dependencies = new LinkedHashSet<>();

    private WorksheetSyntaxNode[] createWorksheetNodes(TablePartProcessor tablePartProcessor,
                                                       XlsWorkbookSourceCodeModule workbookSourceModule) {
        var source = workbookSourceModule.getSource();

        if (VirtualSourceCodeModule.SOURCE_URI.equals(source.getUri())) {
            var nSheets = workbookSourceModule.getWorkbookLoader().getNumberOfSheets();
            WorksheetSyntaxNode[] sheetNodes = new WorksheetSyntaxNode[nSheets];

            for (var i = 0; i < nSheets; i++) {
                var sheetSource = new XlsSheetSourceCodeModule(i, workbookSourceModule);
                var tables = new XlsSheetGridModel(sheetSource).getTables();
                sheetNodes[i] = createWorksheetSyntaxNode(tablePartProcessor, sheetSource, tables);
            }
            return sheetNodes;
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
            // The resource can be inside jar, zip or other virtual file system.
            // Example of such case is AlgorithmTableSpecification.xls.
            path = null;
        }
        try (ExcelReader excelReader = path == null ? factory.create(source.getByteStream()) : factory.create(path)) {
            List<? extends SheetDescriptor> sheets = excelReader.getSheets();
            var use1904Windowing = excelReader.isUse1904Windowing();

            var nSheets = sheets.size();
            WorksheetSyntaxNode[] sheetNodes = new WorksheetSyntaxNode[nSheets];

            for (var i = 0; i < nSheets; i++) {
                final var sheet = sheets.get(i);
                var sheetSource = new SequentialXlsSheetSourceCodeModule(workbookSourceModule,
                        sheet);
                var cells = excelReader.getCells(sheet);
                var tables = new ParsedGrid(path, sheetSource, sheet, cells, use1904Windowing).getTables();
                sheetNodes[i] = createWorksheetSyntaxNode(tablePartProcessor, sheetSource, tables);
            }

            return sheetNodes;
        }
    }

    private void addError(SyntaxNodeException error) {
        errors.add(error);
    }

    public IParsedCode parse(IOpenSourceCodeModule source) {

        preprocessWorkbook(source);

        var workbooksArray = workbookNodes.toArray(new WorkbookSyntaxNode[0]);
        var syntaxNode = new XlsModuleSyntaxNode(workbooksArray,
                source,
                Collections.unmodifiableCollection(imports));

        var parsingErrors = errors.toArray(SyntaxNodeException.EMPTY_ARRAY);

        return new ParsedCode(syntaxNode, source, parsingErrors, messages, dependencies.toArray(new IDependency[0]));
    }

    private void preprocessEnvironmentTable(TableSyntaxNode tableSyntaxNode, XlsSheetSourceCodeModule source) {

        var logicalTable = tableSyntaxNode.getTable();

        var height = logicalTable.getHeight();

        for (var i = 1; i < height; i++) {
            var row = logicalTable.getRow(i);

            var value = row.getColumn(0).getSource().getCell(0, 0).getStringValue();
            if (StringUtils.isNotBlank(value)) {
                value = value.trim();
            }

            if (IXlsTableNames.LANG_PROPERTY.equals(value)) {
                // Drop support "language" property
            } else if (IXlsTableNames.DEPENDENCY.equals(value)) {
                // process module dependency
                //
                preprocessDependency(tableSyntaxNode, row.getSource());
            } else if (IXlsTableNames.INCLUDE_TABLE.equals(value)) {
                preprocessIncludeTable(row.getSource(), source);
            } else if (IXlsTableNames.IMPORT_PROPERTY.equals(value)) {
                preprocessImportTable(row.getSource());
            } else if (ParserUtils.isBlankOrCommented(value)) {
                // ignore comment
                log.debug("Comment: {}", value);
            } else {
                var message = "Error in Environment table: unrecognized keyword '%s'".formatted(value);
                messages.add(OpenLMessagesUtils.newWarnMessage(message, tableSyntaxNode));
            }
        }
    }

    private void preprocessDependency(TableSyntaxNode tableSyntaxNode, IGridTable gridTable) {

        var height = gridTable.getHeight();

        for (var i = 0; i < height; i++) {
            var dependency = gridTable.getCell(1, i).getStringValue();
            if (StringUtils.isNotBlank(dependency)) {
                dependency = dependency.trim();

                var node = new IdentifierNode(IXlsTableNames.DEPENDENCY,
                        LocationUtils.createTextInterval(dependency),
                        dependency,
                        new GridCellSourceCodeModule(gridTable, 1, i, null));
                node.setParent(tableSyntaxNode);
                var moduleDependency = new Dependency(DependencyType.MODULE, node);
                dependencies.add(moduleDependency);
            }
        }
    }

    private void preprocessImportTable(IGridTable table) {
        var height = table.getHeight();

        for (var i = 0; i < height; i++) {
            var singleImport = table.getCell(1, i).getStringValue();
            if (StringUtils.isNotBlank(singleImport)) {
                addImport(singleImport.trim());
            }
        }
    }

    private void addImport(String singleImport) {
        imports.add(singleImport);
    }

    protected static String getParentAndMergePaths(String p1, String p2) {
        p1 = p1.replaceAll("\\\\", "/");
        p2 = p2.replaceAll("\\\\", "/");

        var pp1 = p1.split("/");
        var pp2 = p2.split("/");
        var result = new ArrayList<String>();

        int len = p1.endsWith("/") ? pp1.length : pp1.length - 1;

        for (var i = 0; i < len; i++) {
            if (pp1[i].equals(".")) {
                continue;
            }
            if (pp1[i].equals("..")) {
                if (!result.isEmpty() && !result.getLast().equals("..")) {
                    result.removeLast();
                    continue;
                }
            }
            result.add(pp1[i]);
        }

        for (String s : pp2) {
            if (s.equals(".")) {
                continue;
            }
            if (!result.isEmpty() && s.equals("..")) {
                result.removeLast();
                continue;
            }
            result.add(s);
        }

        return String.join("/", result);
    }

    private void preprocessIncludeTable(IGridTable table, XlsSheetSourceCodeModule sheetSource) {

        var height = table.getHeight();

        for (var i = 0; i < height; i++) {

            var include = table.getCell(1, i).getStringValue();

            if (StringUtils.isNotBlank(include)) {
                try {
                    var newURL = getParentAndMergePaths(sheetSource.getWorkbookSource().getFileUri(),
                            StringTool.encodeURL(include));
                    var src = new URLSourceCodeModule(new URI(newURL).toURL());
                    preprocessWorkbook(src);
                } catch (Exception t) {
                    registerIncludeError(table, i, include, t);
                }
            }
        }
    }

    private void registerIncludeError(IGridTable table, int i, String include, Exception t) {
        SyntaxNodeException se = SyntaxNodeExceptionUtils.createError("Include '" + include + "' is not found.",
                t,
                LocationUtils.createTextInterval(include),
                new GridCellSourceCodeModule(table, 1, i, null));
        addError(se);
    }

    private TableSyntaxNode preprocessTable(IGridTable table,
                                            XlsSheetSourceCodeModule source,
                                            TablePartProcessor tablePartProcessor) throws OpenLCompilationException {

        TableSyntaxNode tsn = XlsHelper.createTableSyntaxNode(table, source);

        var type = tsn.getType();
        if (type.equals(XlsNodeTypes.XLS_ENVIRONMENT.toString())) {
            preprocessEnvironmentTable(tsn, source);
        } else if (type.equals(XlsNodeTypes.XLS_TABLEPART.toString())) {
            try {
                tablePartProcessor.register(table, source);
            } catch (Exception | LinkageError t) {
                tsn = new TableSyntaxNode(XlsNodeTypes.XLS_OTHER
                        .toString(), tsn.getGridLocation(), source, table, tsn.getHeader());
                SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(t, tsn);
                addError(sne);
            }
        }

        return tsn;
    }

    private void preprocessWorkbook(IOpenSourceCodeModule source) {

        var uri = source.getUri();

        if (preprocessedWorkBooks.contains(uri)) {
            return;
        }

        preprocessedWorkBooks.add(uri);

        var tablePartProcessor = new TablePartProcessor();
        var workbookSourceModule = new XlsWorkbookSourceCodeModule(source);
        var sheetNodes = createWorksheetNodes(tablePartProcessor, workbookSourceModule);

        workbookNodes.add(createWorkbookNode(tablePartProcessor, workbookSourceModule, sheetNodes));
        messages.addAll(tablePartProcessor.getMessages());
    }

    private WorkbookSyntaxNode createWorkbookNode(TablePartProcessor tablePartProcessor,
                                                  XlsWorkbookSourceCodeModule workbookSourceModule,
                                                  WorksheetSyntaxNode[] sheetNodes) {
        TableSyntaxNode[] mergedNodes = {};
        try {
            List<TablePart> tableParts = tablePartProcessor.mergeAllNodes();
            var n = tableParts.size();
            mergedNodes = new TableSyntaxNode[n];
            for (var i = 0; i < n; i++) {
                mergedNodes[i] = preprocessTable(tableParts.get(i).getTable(),
                        tableParts.get(i).getSource(),
                        tablePartProcessor);
            }
        } catch (OpenLCompilationException e) {
            messages.add(OpenLMessagesUtils.newErrorMessage(e));
        }

        return new WorkbookSyntaxNode(sheetNodes, mergedNodes, workbookSourceModule);
    }

    private WorksheetSyntaxNode createWorksheetSyntaxNode(TablePartProcessor tablePartProcessor,
                                                          XlsSheetSourceCodeModule sheetSource,
                                                          IGridTable[] tables) {
        var tableNodes = new ArrayList<TableSyntaxNode>();

        for (IGridTable table : tables) {

            TableSyntaxNode tsn;

            try {
                tsn = preprocessTable(table, sheetSource, tablePartProcessor);
                tableNodes.add(tsn);
            } catch (OpenLCompilationException e) {
                messages.add(OpenLMessagesUtils.newErrorMessage(e));
            }
        }

        return new WorksheetSyntaxNode(tableNodes.toArray(TableSyntaxNode.EMPTY_ARRAY), sheetSource);
    }
}
