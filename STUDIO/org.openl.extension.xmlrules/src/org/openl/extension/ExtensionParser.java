package org.openl.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.BaseParser;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
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
 *
 * @param <T> Extension project model
 */
public abstract class ExtensionParser<T> extends BaseParser {
    private final Logger log = LoggerFactory.getLogger(ExtensionParser.class);

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        T project = load(source);

        ISyntaxNode syntaxNode = null;
        List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

        try {
            XlsWorkbookSourceCodeModule workbookSourceCodeModule = getWorkbookSourceCodeModule(project, source);

            WorkbookSyntaxNode[] workbooksArray = getWorkbooks(project, workbookSourceCodeModule);
            syntaxNode = new XlsModuleSyntaxNode(workbooksArray,
                    workbookSourceCodeModule,
                    null,
                    null,
                    Collections.<String>emptyList(),
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

    protected WorkbookSyntaxNode[] getWorkbooks(T project, XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
        // TODO Get sheet number and name from project: sheet name is used as category name
        XlsSheetSourceCodeModule sheetSource = new XlsSheetSourceCodeModule(0, workbookSourceCodeModule);
        WorksheetSyntaxNode sheetNode = getWorksheet(sheetSource, project);
        WorksheetSyntaxNode[] sheetNodes = { sheetNode };

        // TODO: Add TableParts
        TableSyntaxNode[] mergedNodes = {};

        return new WorkbookSyntaxNode[] { new WorkbookSyntaxNode(sheetNodes, mergedNodes, workbookSourceCodeModule) };
    }

    protected WorksheetSyntaxNode getWorksheet(XlsSheetSourceCodeModule sheetSource, T project) {
        IGridTable[] tables = getAllGridTables(sheetSource, project);
        List<TableSyntaxNode> tableNodes = new ArrayList<TableSyntaxNode>();

        for (IGridTable table : tables) {
            try {
                tableNodes.add(XlsHelper.createTableSyntaxNode(table, sheetSource));
            } catch (OpenLCompilationException e) {
                OpenLMessagesUtils.addError(e);
            }
        }

        return new WorksheetSyntaxNode(tableNodes.toArray(new TableSyntaxNode[tableNodes.size()]), sheetSource);
    }

    /**
     * Load the project from source
     *
     * @param source source
     * @return loaded project
     */
    protected abstract T load(IOpenSourceCodeModule source);

    /**
     * Wrap source to XlsWorkbookSourceCodeModule
     */
    protected abstract XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule(T project,
            IOpenSourceCodeModule source) throws OpenLCompilationException;

    /**
     * Gets all grid tables from the sheet.
     */
    protected abstract IGridTable[] getAllGridTables(XlsSheetSourceCodeModule sheetSource, T project);
}
