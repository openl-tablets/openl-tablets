package org.openl.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.extension.xmlrules.XmlSheetSourceCodeModule;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.project.XmlRulesModuleSourceCodeModule;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.*;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
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
        String internalWorkbookPath = ((XmlRulesModuleSourceCodeModule) source).getInternalModulePath();
        ExtensionModule module = load(source);

        ISyntaxNode syntaxNode = null;
        List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

        try {
            XlsWorkbookSourceCodeModule workbookSourceCodeModule = getWorkbookSourceCodeModule(module, source);

            WorkbookSyntaxNode[] workbooksArray = getWorkbooks(module, workbookSourceCodeModule, internalWorkbookPath);
            syntaxNode = new XlsModuleSyntaxNode(workbooksArray,
                    workbookSourceCodeModule,
                    null,
                    null,
                    getImports(),
                    Collections.<IdentifierNode>emptyList());
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

        return new ParsedCode(syntaxNode,
                source,
                parsingErrors,
                new IDependency[] {});
    }

    protected List<String> getImports() {
        return Collections.emptyList();
    }

    protected WorkbookSyntaxNode[] getWorkbooks(ExtensionModule module,
            XlsWorkbookSourceCodeModule workbookSourceCodeModule,
            String internalWorkbookPath) {
        TablePartProcessor tablePartProcessor = new TablePartProcessor();

        List<WorkbookSyntaxNode> workbookSyntaxNodes = new ArrayList<WorkbookSyntaxNode>();
        List<WorksheetSyntaxNode> sheetNodeList = new ArrayList<WorksheetSyntaxNode>();

        for (LazyWorkbook workbook : module.getWorkbooks()) {
            if (!internalWorkbookPath.equals(workbook.getXlsFileName())) {
                continue;
            }
            List<Sheet> sheets = workbook.getSheets();
            for (int i = 0; i < sheets.size(); i++) {
                Sheet sheet = sheets.get(i);
                // Sheet name is used as category name in WebStudio
                XlsSheetSourceCodeModule sheetSource = new XmlSheetSourceCodeModule(i,
                        workbookSourceCodeModule,
                        workbook);
                sheetNodeList.add(getWorksheet(sheetSource, workbook, sheet, module, tablePartProcessor));
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
            TablePartProcessor tablePartProcessor) {
        IGridTable[] tables = getAllGridTables(sheetSource, module, workbook, sheet);
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
            TablePartProcessor tablePartProcessor) throws
                                                                                                    OpenLCompilationException {
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
            LazyWorkbook workbook, Sheet sheet);
}
