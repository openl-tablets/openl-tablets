package org.openl.rules.indexer;

import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

/**
 * Parser for sheet. Parses the sheet to table it contains.
 * 
 */
public class WorksheetIndexParser implements IIndexParser {

    public String getCategory() {
        return IDocumentType.WORKSHEET.getCategory();
    }

    public String getType() {
        return IDocumentType.WORKSHEET.getType();
    }

    /**
     * Try to process root as sheet {@link XlsSheetSourceCodeModule} and call
     * {@link WorksheetIndexParser#parseSheet(XlsSheetSourceCodeModule)}.
     * 
     * @return Tables from the sheet as array of {@link IIndexElement}.
     */
    public IIndexElement[] parse(IIndexElement root) {
        XlsSheetSourceCodeModule sheetSrc = (XlsSheetSourceCodeModule) root;
        return parseSheet(sheetSrc);
    }

    /**
     * Parses the sheet to table it contains.
     * 
     * @param sheetSrc Sheet for parsing.
     * @return Tables from the sheet.
     */
    public TableSyntaxNode[] parseSheet(XlsSheetSourceCodeModule sheetSrc) {

        XlsSheetGridModel xlsGrid = new XlsSheetGridModel(sheetSrc);

        IGridTable[] tables = xlsGrid.getTables();

        TableSyntaxNode[] nodes = new TableSyntaxNode[tables.length];

        for (int i = 0; i < nodes.length; i++) {

            IGridTable table = tables[i];


            try {
                TableSyntaxNode tableSyntaxNode = XlsHelper.createTableSyntaxNode(table, sheetSrc);
                nodes[i] = tableSyntaxNode;
            } catch (OpenLCompilationException e) {

                // Add error.
                //
                GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Cannot parse table header",
                    e,
                    null,
                    src);
                OpenLMessagesUtils.addError(error);

                // Continue tables parsing.
                // 
                continue;
            }

        }

        return nodes;
    }

}
