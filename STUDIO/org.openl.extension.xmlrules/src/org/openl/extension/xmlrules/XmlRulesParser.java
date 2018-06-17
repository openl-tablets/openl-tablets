package org.openl.extension.xmlrules;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.lazy.LazyAttributes;
import org.openl.extension.xmlrules.model.lazy.LazyCells;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.SheetHolder;
import org.openl.extension.xmlrules.model.single.SheetImpl;
import org.openl.extension.xmlrules.model.single.node.NamedRange;
import org.openl.extension.xmlrules.parsing.ArrayCellExpressionGridBuilder;
import org.openl.extension.xmlrules.parsing.CellExpressionGridBuilder;
import org.openl.extension.xmlrules.parsing.DataInstanceGridBuilder;
import org.openl.extension.xmlrules.parsing.EnvironmentGridBuilder;
import org.openl.extension.xmlrules.parsing.FunctionGridBuilder;
import org.openl.extension.xmlrules.parsing.TableGridBuilder;
import org.openl.extension.xmlrules.parsing.TypeGridBuilder;
import org.openl.extension.xmlrules.project.XmlRulesModule;
import org.openl.extension.xmlrules.project.XmlRulesModuleSourceCodeModule;
import org.openl.extension.xmlrules.project.XmlRulesModuleSyntaxNode;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.BaseParser;
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
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO Reduce complexity
public class XmlRulesParser extends BaseParser {
    private final Logger log = LoggerFactory.getLogger(XmlRulesParser.class);

    public XmlRulesParser() {
    }

    /**
     * Wrap source to XlsWorkbookSourceCodeModule
     */
    protected XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule(ExtensionModule extensionModule,
            IOpenSourceCodeModule source) throws OpenLCompilationException {
        try {
            // TODO Check the cases when source can be UrlSourceCodeModule or
            // another one.
            File projectFolder = new File(new File(new URI(source.getUri())).getParent());

            return new XlsWorkbookSourceCodeModule(source,
                new LazyXmlRulesWorkbookLoader(projectFolder, extensionModule));
        } catch (URISyntaxException e) {
            throw new OpenLCompilationException(e.getMessage(), e);
        }
    }

    /**
     * Gets all grid tables from the sheet.
     */
    protected IGridTable[] getAllGridTables(XlsSheetSourceCodeModule sheetSource,
            ExtensionModule module,
            LazyWorkbook workbook,
            Sheet sheet,
            XmlRulesModuleSourceCodeModule sourceCodeModule,
            List<ParseError> parseErrors,
            Collection<OpenLMessage> messages) {
        String uri = sheetSource.getUri();
        LazyXmlRulesWorkbookLoader workbookLoader = (LazyXmlRulesWorkbookLoader) sheetSource.getWorkbookSource()
            .getWorkbookLoader();

        StringGridBuilder gridBuilder = new StringGridBuilder(uri, workbookLoader.getExtensionModule().getFileName());

        if (workbook.getXlsFileName().equals(ExtensionDescriptor.TYPES_WORKBOOK)) {
            TypeGridBuilder.build(gridBuilder, sheet, messages);
        } else {
            DataInstanceGridBuilder.build(gridBuilder, module, sheet, messages);
            TableGridBuilder.build(gridBuilder, module, sheet, messages);
            FunctionGridBuilder.build(gridBuilder, module, sheet, messages);
            CellExpressionGridBuilder.build(sheetSource, gridBuilder, sheet, parseErrors, messages);
            ArrayCellExpressionGridBuilder.build(gridBuilder, sheet, parseErrors, messages);

            if (sheet.getId() == ExtensionDescriptor.MAIN_SHEET_NUMBER) {
                EnvironmentGridBuilder.build(gridBuilder, sourceCodeModule, messages);
            }
        }

        return gridBuilder.build().getTables();
    }

    private void initNamedRanges(Sheet sheet) {
        if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
            sheet = ((SheetHolder) sheet).getInternalSheet();
        }
        if (CollectionUtils.isEmpty(sheet.getCells())) {
            return;
        }
        ProjectData projectData = ProjectData.getCurrentInstance();

        for (LazyCells cells : sheet.getCells()) {
            for (NamedRange namedRange : cells.getNamedRanges()) {
                projectData.addNamedRange(namedRange.getName(), namedRange.getRange());
            }
        }
    }

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        XmlRulesModuleSourceCodeModule sourceCodeModule = (XmlRulesModuleSourceCodeModule) source;
        XmlRulesModule openlModule = sourceCodeModule.getModule();
        ZipFileXmlDeserializer zipFileXmlDeserializer = new ZipFileXmlDeserializer(source.getUri());
        ExtensionModule module = zipFileXmlDeserializer.deserialize();

        ISyntaxNode syntaxNode = null;
        List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();
        Collection<OpenLMessage> messages = new LinkedHashSet<>();

        ProjectData projectData = new ProjectData();
        ProjectData.setCurrentInstance(projectData);
        projectData.setAttributes(new LazyAttributes(zipFileXmlDeserializer.getFile()));

        try {
            XlsWorkbookSourceCodeModule workbookSourceCodeModule = getWorkbookSourceCodeModule(module, source);

            initTypes(module);

            WorkbookSyntaxNode[] workbooksArray = getWorkbooks(module,
                workbookSourceCodeModule,
                sourceCodeModule,
                messages);
            syntaxNode = new XmlRulesModuleSyntaxNode(workbooksArray,
                workbookSourceCodeModule,
                null,
                getImports(),
                projectData);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            String message = String
                .format("Failed to open extension module: %s. Reason: %s", source.getUri(), e.getMessage());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, e, null);
            errors.add(error);

            syntaxNode = new XmlRulesModuleSyntaxNode(new WorkbookSyntaxNode[0],
                sourceCodeModule,
                null,
                getImports(),
                projectData);
        } finally {
            ProjectData.clearUnmarshaller();
        }

        SyntaxNodeException[] parsingErrors = errors.toArray(new SyntaxNodeException[errors.size()]);

        List<IDependency> dependencies = new ArrayList<IDependency>();
        if (!sourceCodeModule.getInternalModulePath().equals(ExtensionDescriptor.TYPES_WORKBOOK)) {
            String name = sourceCodeModule.getModuleName();
            String moduleName = name.substring(0, name.lastIndexOf(".")) + "." + ExtensionDescriptor.TYPES_WORKBOOK
                .substring(0, ExtensionDescriptor.TYPES_WORKBOOK.lastIndexOf("."));
            IdentifierNode node = new IdentifierNode(IXlsTableNames.DEPENDENCY, null, moduleName, null);
            dependencies.add(new Dependency(DependencyType.MODULE, node));
            List<String> dependenciesList = openlModule.getExtension().getDependencies();
            if (dependenciesList != null) {
                for (String dependency : dependenciesList) {
                    dependencies.add(new Dependency(DependencyType.MODULE,
                        new IdentifierNode(IXlsTableNames.DEPENDENCY, null, dependency, null)));
                }
            }
        }

        return new ParsedCode(syntaxNode,
            source,
            parsingErrors,
            messages,
            dependencies.toArray(new IDependency[dependencies.size()]));
    }

    private void initTypes(ExtensionModule module) {
        ProjectData projectData = ProjectData.getCurrentInstance();
        for (LazyWorkbook workbook : module.getInternalWorkbooks()) {
            for (Sheet s : workbook.getSheets()) {
                for (Type type : s.getTypes()) {
                    projectData.addType(type);
                }
                for (Function function : s.getFunctions()) {
                    projectData.addFunction(s, function);
                }
                for (Table table : s.getTables()) {
                    projectData.addTable(s, table);
                }
            }
        }
    }

    protected Collection<String> getImports() {
        return Collections.singletonList("org.openl.rules.enumeration");
    }

    protected Collection<String> getLibraries() {
        return Collections.emptySet();
    }

    protected WorkbookSyntaxNode[] getWorkbooks(ExtensionModule module,
            XlsWorkbookSourceCodeModule workbookSourceCodeModule,
            XmlRulesModuleSourceCodeModule sourceCodeModule,
            Collection<OpenLMessage> messages) {
        TablePartProcessor tablePartProcessor = new TablePartProcessor();

        List<WorkbookSyntaxNode> workbookSyntaxNodes = new ArrayList<WorkbookSyntaxNode>();
        List<WorksheetSyntaxNode> sheetNodeList = new ArrayList<WorksheetSyntaxNode>();

        for (LazyWorkbook workbook : module.getWorkbooks()) {
            if (!sourceCodeModule.getInternalModulePath().equals(workbook.getXlsFileName())) {
                continue;
            }
            if (workbook.getXlsFileName().equals(ExtensionDescriptor.TYPES_WORKBOOK)) {
                ArrayList<Sheet> sheets = new ArrayList<Sheet>();
                workbook.setSheets(sheets);
                SheetImpl sheet = new SheetImpl();
                sheet.setName(ExtensionDescriptor.TYPES_SHEET);
                ArrayList<Type> types = new ArrayList<Type>();
                sheet.setTypes(types);
                sheets.add(sheet);
                for (LazyWorkbook w : module.getInternalWorkbooks()) {
                    for (Sheet s : w.getSheets()) {
                        types.addAll(s.getTypes());
                    }
                }
            } else if (workbook.getXlsFileName().equals(ExtensionDescriptor.MAIN_WORKBOOK)) {
                ArrayList<Sheet> sheets = new ArrayList<Sheet>();
                workbook.setSheets(sheets);
                int id = 1;
                for (LazyWorkbook w : module.getInternalWorkbooks()) {
                    for (Sheet s : w.getSheets()) {
                        SheetImpl sheet = new SheetImpl();
                        sheet.setWorkbookName(s.getWorkbookName());
                        sheet.setId(id++);
                        sheet.setName(w.getXlsFileName() + "." + s.getName());
                        sheet.setTables(s.getTables());
                        sheet.setFunctions(s.getFunctions());
                        sheet.setDataInstances(s.getDataInstances());
                        sheet.setCells(s.getCells());
                        sheet.setInternalSheet(s);
                        sheets.add(sheet);
                    }
                }
            }
            List<Sheet> sheets = workbook.getSheets();

            for (Sheet sheet : sheets) {
                try {
                    initNamedRanges(sheet);
                } catch (RuntimeException e) {
                    log.error(e.getMessage(), e);
                    messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
                }
            }

            for (int i = 0; i < sheets.size(); i++) {
                Sheet sheet = sheets.get(i);
                // Sheet name is used as category name in WebStudio
                XlsSheetSourceCodeModule sheetSource = new XmlSheetSourceCodeModule(i,
                    workbookSourceCodeModule,
                    workbook);
                sheetNodeList.add(
                    getWorksheet(sheetSource, workbook, sheet, module, tablePartProcessor, sourceCodeModule, messages));
            }
        }

        WorksheetSyntaxNode[] sheetNodes = sheetNodeList.toArray(new WorksheetSyntaxNode[sheetNodeList.size()]);

        TableSyntaxNode[] mergedNodes = {};
        try {
            List<TablePart> tableParts = tablePartProcessor.mergeAllNodes();
            int n = tableParts.size();
            mergedNodes = new TableSyntaxNode[n];
            for (int i = 0; i < n; i++) {
                mergedNodes[i] = preprocessTable(tableParts.get(i).getTable(),
                    tableParts.get(i).getSource(),
                    tablePartProcessor,
                    messages);
            }
        } catch (OpenLCompilationException e) {
            messages.add(OpenLMessagesUtils.newErrorMessage(e));
        }

        workbookSyntaxNodes.add(new WorkbookSyntaxNode(sheetNodes, mergedNodes, workbookSourceCodeModule));

        return workbookSyntaxNodes.toArray(new WorkbookSyntaxNode[workbookSyntaxNodes.size()]);
    }

    protected WorksheetSyntaxNode getWorksheet(XlsSheetSourceCodeModule sheetSource,
            LazyWorkbook workbook,
            Sheet sheet,
            ExtensionModule module,
            TablePartProcessor tablePartProcessor,
            XmlRulesModuleSourceCodeModule sourceCodeModule,
            Collection<OpenLMessage> messages) {
        List<ParseError> parseErrors = new ArrayList<ParseError>();
        IGridTable[] tables = getAllGridTables(sheetSource,
            module,
            workbook,
            sheet,
            sourceCodeModule,
            parseErrors,
            messages);
        List<TableSyntaxNode> tableNodes = new ArrayList<TableSyntaxNode>();

        for (IGridTable table : tables) {
            try {
                tableNodes.add(preprocessTable(table, sheetSource, tablePartProcessor, messages));
            } catch (OpenLCompilationException e) {
                messages.add(OpenLMessagesUtils.newErrorMessage(e));
            }
        }

        for (ParseError parseError : parseErrors) {
            int row = parseError.getRow();
            int column = parseError.getColumn();

            for (TableSyntaxNode tsn : tableNodes) {
                IGridRegion region = tsn.getGridTable().getRegion();
                int top = region.getTop();
                int bottom = region.getBottom();
                int left = region.getLeft();
                int right = region.getRight();

                if (top <= row && bottom >= row && left <= column && right >= column) {
                    SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(parseError.getMessage(), tsn);
                    tsn.addError(sne);
                    messages.add(OpenLMessagesUtils.newErrorMessage(sne));
                    break;
                }
            }
        }

        return new WorksheetSyntaxNode(tableNodes.toArray(new TableSyntaxNode[tableNodes.size()]), sheetSource);
    }

    private TableSyntaxNode preprocessTable(IGridTable table,
            XlsSheetSourceCodeModule source,
            TablePartProcessor tablePartProcessor,
            Collection<OpenLMessage> messages) throws OpenLCompilationException {
        TableSyntaxNode tsn = XlsHelper.createTableSyntaxNode(table, source);
        String type = tsn.getType();
        if (type.equals(XlsNodeTypes.XLS_TABLEPART.toString())) {
            try {
                tablePartProcessor.register(table, source);
            } catch (Exception e) {
                tsn = new TableSyntaxNode(XlsNodeTypes.XLS_OTHER
                    .toString(), tsn.getGridLocation(), source, table, tsn.getHeader());
                SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(e, tsn);
                tsn.addError(sne);
                messages.add(OpenLMessagesUtils.newErrorMessage(sne));
            }
        }
        return tsn;
    }

}
