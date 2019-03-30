package org.openl.rules.lang.xls;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.calc.CellsHeaderExtractor;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeHelper;
import org.openl.rules.table.ILogicalTable;

public class SpreadsheetTableSyntaxNodeRelationsDeterminer implements TableSyntaxNodeRelationsDeterminer {

    @Override
    public boolean determine(TableSyntaxNode node, TableSyntaxNode dependsOnNode) {
        if (isSpreadsheet(node) && isSpreadsheet(dependsOnNode)) {
            // Compare both spreadsheets table syntax nodes.
            // Check if there are usages of custom spreadsheet type
            //
            CellsHeaderExtractor extractor1 = extractNames(node);
            if (extractor1 == null) {
                return false;
            }

            String methodName2 = TableSyntaxNodeHelper.getTableName(dependsOnNode);
            if (StringUtils.isNotBlank(methodName2) && extractor1.getDependentSignatureSpreadsheetTypes()
                .contains(methodName2)) {
                return true;
            }
            return false;
        }
        throw new IllegalStateException("Spreadsheet tables are supported only!");
    }

    private static boolean isSpreadsheet(TableSyntaxNode o1) {
        return XlsNodeTypes.XLS_SPREADSHEET.equals(o1.getNodeType());
    }

    private static CellsHeaderExtractor extractNames(TableSyntaxNode tableSyntaxNode) {
        CellsHeaderExtractor extractor;

        // try to get previously stored extractor
        //
        SpreadsheetHeaderNode header = (SpreadsheetHeaderNode) tableSyntaxNode.getHeader();
        extractor = header.getCellHeadersExtractor();

        if (extractor == null) {
            ILogicalTable body = tableSyntaxNode.getTableBody();
            if (body != null) {
                extractor = new CellsHeaderExtractor(TableSyntaxNodeHelper.getSignature(tableSyntaxNode),
                        body.getRow(0).getColumns(1),
                        body.getColumn(0).getRows(1));

                // set cells header extractor to the table syntax node, to avoid
                // extracting several times
                //
                header.setCellHeadersExtractor(extractor);
            }
        }

        return extractor;
    }
}
