package org.openl.rules.indexer;

import org.openl.rules.lang.xls.XlsLoader;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.TokenizerParser;

public class WorksheetIndexParser implements IIndexParser {

    public String getCategory() {
        return IDocumentType.WORKSHEET.getCategory();
    }

    public String getType() {
        return IDocumentType.WORKSHEET.getType();
    }

    public IIndexElement[] parse(IIndexElement root) {
        XlsSheetSourceCodeModule sheetSrc = (XlsSheetSourceCodeModule) root;
        return parseSheet(sheetSrc);
    }

    public TableSyntaxNode[] parseSheet(XlsSheetSourceCodeModule sheetSrc) {

        XlsSheetGridModel xlsGrid = new XlsSheetGridModel(sheetSrc);

        IGridTable[] tables = new GridSplitter(xlsGrid).split();

        TableSyntaxNode[] nodes = new TableSyntaxNode[tables.length];

        for (int i = 0; i < nodes.length; i++) {
            IGridTable table = tables[i];

            GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);

            IdentifierNode parsedHeader = TokenizerParser.firstToken(src, " \n\r");

            String header = parsedHeader.getIdentifier();

            HeaderSyntaxNode headerNode = new HeaderSyntaxNode(src, parsedHeader);

            String xls_type = XlsLoader.tableHeaders().get(header);
            if (xls_type == null) {
                xls_type = "N/A";
            }

            nodes[i] = new TableSyntaxNode(xls_type, new GridLocation(table), sheetSrc, table, headerNode);
        }

        return nodes;
    }

}
