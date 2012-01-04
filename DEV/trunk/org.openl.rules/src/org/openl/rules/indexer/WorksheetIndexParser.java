package org.openl.rules.indexer;

import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.XlsLoader;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * Parser for sheet. Parses the sheet to table it contains.
 * 
 */
public class WorksheetIndexParser implements IIndexParser {

    private static final String NOT_AVAILABLE = "N/A";

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
            GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);

            IdentifierNode parsedHeader;

            try {
                parsedHeader = Tokenizer.firstToken(src, " \n\r");
            } catch (OpenLCompilationException e) {

                // Add error.
                //
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Cannot parse table header",
                    e,
                    null,
                    src);
                OpenLMessagesUtils.addError(error);

                // Continue tables parsing.
                // 
                continue;
            }            

            String header = parsedHeader.getIdentifier();
            String xls_type = XlsLoader.getTableHeaders().get(header);

            if (xls_type == null) {
                xls_type = NOT_AVAILABLE;
            }
            
            HeaderSyntaxNode headerNode = HeaderNodeFactory.getHeaderNode(xls_type, src, parsedHeader);

            nodes[i] = new TableSyntaxNode(xls_type, new GridLocation(table), sheetSrc, table, headerNode);
        }

        return nodes;
    }

}
