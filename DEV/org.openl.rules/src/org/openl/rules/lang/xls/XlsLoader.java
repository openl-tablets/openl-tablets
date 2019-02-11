package org.openl.rules.lang.xls;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.utils.ParserUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.PathTool;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author snshor
 */
public class XlsLoader {

    private final Logger log = LoggerFactory.getLogger(XlsLoader.class);

    private Collection<String> imports = new HashSet<>();

    private IncludeSearcher includeSeeker;

    private OpenlSyntaxNode openl;

    private List<SyntaxNodeException> errors = new ArrayList<>();
    
    private Collection<OpenLMessage> messages = new LinkedHashSet<>();

    private Set<String> preprocessedWorkBooks = new HashSet<>();

    private List<WorkbookSyntaxNode> workbookNodes = new ArrayList<>();

    private List<IDependency> dependencies = new ArrayList<>();

    public XlsLoader(IncludeSearcher includeSeeker) {
        this.includeSeeker = includeSeeker;
    }

    private void addError(SyntaxNodeException error) {
        errors.add(error);
    }

    public IParsedCode parse(IOpenSourceCodeModule source) {

        preprocessWorkbook(source);

        addInnerImports();

        WorkbookSyntaxNode[] workbooksArray = workbookNodes.toArray(new WorkbookSyntaxNode[workbookNodes.size()]);
        XlsModuleSyntaxNode syntaxNode = new XlsModuleSyntaxNode(workbooksArray,
            source,
            openl,
            Collections.unmodifiableCollection(imports));

        SyntaxNodeException[] parsingErrors = errors.toArray(new SyntaxNodeException[errors.size()]);

        return new ParsedCode(syntaxNode,
            source,
            parsingErrors,
            messages,
            dependencies.toArray(new IDependency[dependencies.size()]));
    }

    private void preprocessEnvironmentTable(TableSyntaxNode tableSyntaxNode, XlsSheetSourceCodeModule source) {

        ILogicalTable logicalTable = tableSyntaxNode.getTable();

        int height = logicalTable.getHeight();

        for (int i = 1; i < height; i++) {
            ILogicalTable row = logicalTable.getRow(i);

            String value = row.getColumn(0).getSource().getCell(0, 0).getStringValue();
            if (StringUtils.isNotBlank(value)) {
                value = value.trim();
            }

            if (IXlsTableNames.LANG_PROPERTY.equals(value)) {
                preprocessOpenlTable(row.getSource(), source);
            } else if (IXlsTableNames.DEPENDENCY.equals(value)) {
                // process module dependency
                //
                preprocessDependency(tableSyntaxNode, row.getSource());
            } else if (IXlsTableNames.INCLUDE_TABLE.equals(value)) {
                preprocessIncludeTable(tableSyntaxNode, row.getSource(), source);
            } else if (IXlsTableNames.IMPORT_PROPERTY.equals(value)) {
                preprocessImportTable(row.getSource());
            } else if (ParserUtils.isBlankOrCommented(value)) {
                // ignore comment
                log.debug("Comment: {}", value);
            } else {
                String message = String.format("Error in Environment table: unrecognized keyword '%s'", value);
                messages.add(OpenLMessagesUtils.newWarnMessage(message, tableSyntaxNode));
            }
        }
    }

    private void preprocessDependency(TableSyntaxNode tableSyntaxNode, IGridTable gridTable) {

        int height = gridTable.getHeight();

        for (int i = 0; i < height; i++) {
            String dependency = gridTable.getCell(1, i).getStringValue();
            if (StringUtils.isNotBlank(dependency)) {
                dependency = dependency.trim();

                IdentifierNode node = new IdentifierNode(IXlsTableNames.DEPENDENCY,
                    LocationUtils.createTextInterval(dependency),
                    dependency,
                    new GridCellSourceCodeModule(gridTable, 1, i, null));
                node.setParent(tableSyntaxNode);
                Dependency moduleDependency = new Dependency(DependencyType.MODULE, node);
                dependencies.add(moduleDependency);
            }
        }
    }

    private void preprocessImportTable(IGridTable table) {
        int height = table.getHeight();

        for (int i = 0; i < height; i++) {
            String singleImport = table.getCell(1, i).getStringValue();
            if (StringUtils.isNotBlank(singleImport)) {
                addImport(singleImport.trim());
            }
        }
    }

    private void addImport(String singleImport) {
        imports.add(singleImport);
    }

    private void addInnerImports() {
        addImport("org.openl.rules.enumeration");
    }

    private void preprocessIncludeTable(TableSyntaxNode tableSyntaxNode,
            IGridTable table,
            XlsSheetSourceCodeModule sheetSource) {

        int height = table.getHeight();

        for (int i = 0; i < height; i++) {

            String include = table.getCell(1, i).getStringValue();

            if (StringUtils.isNotBlank(include)) {
                include = include.trim();
                IOpenSourceCodeModule src = null;

                if (include.startsWith("<")) {
                    try {
                        Matcher matcher = Pattern.compile("<([^<>]+)>").matcher(include);
                        matcher.find();
                        System.out.println(matcher.group(1));

                        src = includeSeeker.findInclude(matcher.group(1));
                    }catch (Exception e) {
                        messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
                        
                    }
                    if (src == null) {
                        registerIncludeError(tableSyntaxNode, table, i, include, null);
                        continue;
                    }
                } else {
                    try {
                        String newURL = PathTool.mergePath(sheetSource.getWorkbookSource().getUri(),
                            StringTool.encodeURL(include));
                        src = new URLSourceCodeModule(new URL(newURL));
                    } catch (Exception t) {
                        registerIncludeError(tableSyntaxNode, table, i, include, t);
                        continue;
                    }
                }

                try {
                    preprocessWorkbook(src);
                } catch (Exception t) {
                    registerIncludeError(tableSyntaxNode, table, i, include, t);
                }
            }
        }
    }

    private void registerIncludeError(TableSyntaxNode tableSyntaxNode,
            IGridTable table,
            int i,
            String include,
            Exception t) {
        SyntaxNodeException se = SyntaxNodeExceptionUtils.createError("Include '" + include + "' is not found",
            t,
            LocationUtils.createTextInterval(include),
            new GridCellSourceCodeModule(table, 1, i, null));
        addError(se);
        tableSyntaxNode.addError(se);
    }

    private void preprocessOpenlTable(IGridTable table, XlsSheetSourceCodeModule source) {
        String openlName = table.getCell(1, 0).getStringValue();
        if (StringUtils.isNotBlank(openlName)) {
            openlName = openlName.trim();
        }
        setOpenl(new OpenlSyntaxNode(openlName, new GridLocation(table), source));
    }

    private TableSyntaxNode preprocessTable(IGridTable table,
            XlsSheetSourceCodeModule source,
            TablePartProcessor tablePartProcessor) throws OpenLCompilationException {

        TableSyntaxNode tsn = XlsHelper.createTableSyntaxNode(table, source);

        String type = tsn.getType();
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
                tsn.addError(sne);
            }
        }

        return tsn;
    }

    private void preprocessWorkbook(IOpenSourceCodeModule source) {

        String uri = source.getUri();

        if (preprocessedWorkBooks.contains(uri)) {
            return;
        }

        preprocessedWorkBooks.add(uri);

        TablePartProcessor tablePartProcessor = new TablePartProcessor();
        XlsWorkbookSourceCodeModule workbookSourceModule = new XlsWorkbookSourceCodeModule(source);
        WorksheetSyntaxNode[] sheetNodes = createWorksheetNodes(tablePartProcessor, workbookSourceModule);

        workbookNodes.add(createWorkbookNode(tablePartProcessor, workbookSourceModule, sheetNodes));
        messages.addAll(tablePartProcessor.getMessages());
    }

    protected WorksheetSyntaxNode[] createWorksheetNodes(TablePartProcessor tablePartProcessor,
            XlsWorkbookSourceCodeModule workbookSourceModule) {
        int nsheets = workbookSourceModule.getWorkbookLoader().getNumberOfSheets();
        WorksheetSyntaxNode[] sheetNodes = new WorksheetSyntaxNode[nsheets];

        for (int i = 0; i < nsheets; i++) {
            XlsSheetSourceCodeModule sheetSource = new XlsSheetSourceCodeModule(i, workbookSourceModule);
            IGridTable[] tables = new XlsSheetGridModel(sheetSource).getTables();
            sheetNodes[i] = createWorksheetSyntaxNode(tablePartProcessor, sheetSource, tables);
        }
        return sheetNodes;
    }

    private WorkbookSyntaxNode createWorkbookNode(TablePartProcessor tablePartProcessor,
            XlsWorkbookSourceCodeModule workbookSourceModule,
            WorksheetSyntaxNode[] sheetNodes) {
        TableSyntaxNode[] mergedNodes = {};
        try {
            List<TablePart> tableParts = tablePartProcessor.mergeAllNodes();
            int n = tableParts.size();
            mergedNodes = new TableSyntaxNode[n];
            for (int i = 0; i < n; i++) {
                mergedNodes[i] = preprocessTable(tableParts.get(i).getTable(),
                    tableParts.get(i).getSource(),
                    tablePartProcessor);
            }
        } catch (OpenLCompilationException e) {
            messages.add(OpenLMessagesUtils.newErrorMessage(e));
        }

        return new WorkbookSyntaxNode(sheetNodes, mergedNodes, workbookSourceModule);
    }

    protected WorksheetSyntaxNode createWorksheetSyntaxNode(TablePartProcessor tablePartProcessor,
            XlsSheetSourceCodeModule sheetSource,
            IGridTable[] tables) {
        List<TableSyntaxNode> tableNodes = new ArrayList<>();

        for (IGridTable table : tables) {

            TableSyntaxNode tsn;

            try {
                tsn = preprocessTable(table, sheetSource, tablePartProcessor);
                tableNodes.add(tsn);
            } catch (OpenLCompilationException e) {
                messages.add(OpenLMessagesUtils.newErrorMessage(e));
            }
        }

        return new WorksheetSyntaxNode(tableNodes.toArray(new TableSyntaxNode[tableNodes.size()]), sheetSource);
    }

    private void setOpenl(OpenlSyntaxNode openl) {

        if (this.openl == null) {
            this.openl = openl;
        } else {
            if (!this.openl.getOpenlName().equals(openl.getOpenlName())) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils
                    .createError("Only one openl statement is allowed", null, openl);
                addError(error);
            }
        }
    }
}
