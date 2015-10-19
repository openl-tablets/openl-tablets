package org.openl.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.XmlSheetSourceCodeModule;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.lazy.LazyExtensionModule;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.SheetImpl;
import org.openl.extension.xmlrules.project.XmlRulesModule;
import org.openl.extension.xmlrules.project.XmlRulesModuleSourceCodeModule;
import org.openl.message.*;
import org.openl.rules.lang.xls.*;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base extension parser
 * TODO: Merge this class with extended one
 */
public abstract class ExtensionParser extends BaseParser {
    private final Logger log = LoggerFactory.getLogger(ExtensionParser.class);

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        XmlRulesModuleSourceCodeModule sourceCodeModule = (XmlRulesModuleSourceCodeModule) source;
        XmlRulesModule openlModule = sourceCodeModule.getModule();
        ExtensionModule module = load(source);

        ISyntaxNode syntaxNode = null;
        List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

        try {
            XlsWorkbookSourceCodeModule workbookSourceCodeModule = getWorkbookSourceCodeModule(module, source);

            for (LazyWorkbook workbook : module.getWorkbooks()) {
                for (Sheet s : workbook.getSheets()) {
                    for (Type type : s.getTypes()) {
                        ProjectData.getCurrentInstance().getTypes().add(type.getName());
                        for (Field field : type.getFields()) {
                            ProjectData.getCurrentInstance().getFields().add(field.getName());
                        }
                    }
                }
            }

            WorkbookSyntaxNode[] workbooksArray = getWorkbooks(module, workbookSourceCodeModule,
                    sourceCodeModule);
            syntaxNode = new XlsModuleSyntaxNode(workbooksArray,
                    workbookSourceCodeModule,
                    null,
                    null,
                    getImports()
            );
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            String message = String.format("Failed to open extension module: %s. Reason: %s",
                    source.getUri(0),
                    e.getMessage());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, e, null);
            errors.add(error);
        }

        SyntaxNodeException[] parsingErrors = errors.toArray(new SyntaxNodeException[errors.size()]);

        List<IDependency> dependencies = new ArrayList<IDependency>();
        if (!sourceCodeModule.getInternalModulePath().equals(LazyExtensionModule.TYPES_WORKBOOK)) {
            String name = sourceCodeModule.getModuleName();
            String moduleName = name.substring(0,
                    name.lastIndexOf(".")) + "." + LazyExtensionModule.TYPES_WORKBOOK.substring(0,
                    LazyExtensionModule.TYPES_WORKBOOK.lastIndexOf("."));
            IdentifierNode node = new IdentifierNode(IXlsTableNames.DEPENDENCY, null, moduleName, null);
            dependencies.add(new Dependency(DependencyType.MODULE, node));
            for (String dependency : openlModule.getExtension().getDependencies()) {
                dependencies.add(new Dependency(DependencyType.MODULE, new IdentifierNode(IXlsTableNames.DEPENDENCY, null, dependency, null)));
            }
        }

        return new ParsedCode(syntaxNode,
                source,
                parsingErrors,
                dependencies.toArray(new IDependency[dependencies.size()]));
    }

    protected List<String> getImports() {
        return Collections.emptyList();
    }

    protected WorkbookSyntaxNode[] getWorkbooks(ExtensionModule module,
            XlsWorkbookSourceCodeModule workbookSourceCodeModule,
            XmlRulesModuleSourceCodeModule sourceCodeModule) {
        TablePartProcessor tablePartProcessor = new TablePartProcessor();

        List<WorkbookSyntaxNode> workbookSyntaxNodes = new ArrayList<WorkbookSyntaxNode>();
        List<WorksheetSyntaxNode> sheetNodeList = new ArrayList<WorksheetSyntaxNode>();

        for (LazyWorkbook workbook : module.getWorkbooks()) {
            if (!sourceCodeModule.getInternalModulePath().equals(workbook.getXlsFileName())) {
                continue;
            }
            if (workbook.getXlsFileName().equals(LazyExtensionModule.TYPES_WORKBOOK)) {
                ArrayList<Sheet> sheets = new ArrayList<Sheet>();
                workbook.setSheets(sheets);
                SheetImpl sheet = new SheetImpl();
                sheet.setName("Types");
                ArrayList<Type> types = new ArrayList<Type>();
                sheet.setTypes(types);
                sheets.add(sheet);
                for (LazyWorkbook w : module.getWorkbooks()) {
                    for (Sheet s : w.getSheets()) {
                        types.addAll(s.getTypes());
                    }
                }
            }
            List<Sheet> sheets = workbook.getSheets();
            for (int i = 0; i < sheets.size(); i++) {
                Sheet sheet = sheets.get(i);
                // Sheet name is used as category name in WebStudio
                XlsSheetSourceCodeModule sheetSource = new XmlSheetSourceCodeModule(i,
                        workbookSourceCodeModule,
                        workbook);
                sheetNodeList.add(getWorksheet(sheetSource, workbook, sheet, module, tablePartProcessor,
                        sourceCodeModule));
            }
        }

        WorksheetSyntaxNode[] sheetNodes = sheetNodeList.toArray(new WorksheetSyntaxNode[sheetNodeList.size()]);

        TableSyntaxNode[] mergedNodes = {};
        try {
            List<TablePart> tableParts = tablePartProcessor.mergeAllNodes();
            int n = tableParts.size();
            mergedNodes = new TableSyntaxNode[n];
            for (int i = 0; i < n; i++) {
                mergedNodes[i] = preprocessTable(tableParts.get(i).getTable(), tableParts.get(i).getSource(),
                        tablePartProcessor);
            }
        } catch (OpenLCompilationException e) {
            OpenLMessagesUtils.addError(e);
        }

        workbookSyntaxNodes.add(new WorkbookSyntaxNode(sheetNodes, mergedNodes, workbookSourceCodeModule));

        return workbookSyntaxNodes.toArray(new WorkbookSyntaxNode[workbookSyntaxNodes.size()]);
    }

    protected WorksheetSyntaxNode getWorksheet(XlsSheetSourceCodeModule sheetSource,
            LazyWorkbook workbook, Sheet sheet,
            ExtensionModule module,
            TablePartProcessor tablePartProcessor, XmlRulesModuleSourceCodeModule sourceCodeModule) {
        IGridTable[] tables = getAllGridTables(sheetSource, module, workbook, sheet, sourceCodeModule);
        List<TableSyntaxNode> tableNodes = new ArrayList<TableSyntaxNode>();

        for (IGridTable table : tables) {
            try {
                tableNodes.add(preprocessTable(table, sheetSource, tablePartProcessor));
            } catch (OpenLCompilationException e) {
                OpenLMessagesUtils.addError(e);
            }
        }

        return new WorksheetSyntaxNode(tableNodes.toArray(new TableSyntaxNode[tableNodes.size()]), sheetSource);
    }

    private TableSyntaxNode preprocessTable(IGridTable table,
            XlsSheetSourceCodeModule source,
            TablePartProcessor tablePartProcessor) throws OpenLCompilationException {
        TableSyntaxNode tsn = XlsHelper.createTableSyntaxNode(table, source);
        String type = tsn.getType();
        if (type.equals(XlsNodeTypes.XLS_TABLEPART.toString())) {
            try {
                tablePartProcessor.register(table, source);
            } catch (Throwable t) {
                SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(t, tsn);
                tsn.addError(sne);
                OpenLMessagesUtils.addError(sne.getMessage());
            }
        }
        return tsn;
    }

    /**
     * Load the project from source
     *
     * @param source source
     * @return loaded project
     */
    protected abstract ExtensionModule load(IOpenSourceCodeModule source);

    /**
     * Wrap source to XlsWorkbookSourceCodeModule
     */
    protected abstract XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule(ExtensionModule project,
            IOpenSourceCodeModule source) throws OpenLCompilationException;

    /**
     * Gets all grid tables from the sheet.
     */
    protected abstract IGridTable[] getAllGridTables(XlsSheetSourceCodeModule sheetSource,
            ExtensionModule module,
            LazyWorkbook workbook, Sheet sheet, XmlRulesModuleSourceCodeModule sourceCodeModule);
}
