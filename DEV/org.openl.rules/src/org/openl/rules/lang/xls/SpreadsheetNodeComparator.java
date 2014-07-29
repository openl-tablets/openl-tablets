package org.openl.rules.lang.xls;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.calc.CellsHeaderExtractor;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Compares spreadsheets. Spreadsheets with dependencies in cells on other
 * Custom Spreadsheet results are considered to be greater.
 * 
 * @author DLiauchuk
 * 
 */
public class SpreadsheetNodeComparator implements Comparator<TableSyntaxNode> {

    public int compare(TableSyntaxNode table1, TableSyntaxNode table2) {
        if (isSpreadsheet(table1) && isSpreadsheet(table2)) {
            // Compare both spreadsheets table syntax nodes.
            // Check if there are usages of custom spreadsheet type
            //
            CellsHeaderExtractor extractor1 = extractNames(table1);

            String methodName2 = getMethodName(table2);
            if (StringUtils.isNotBlank(methodName2)) {
                if (extractor1.getDependentSpreadsheetTypes().contains(methodName2)) {
                    return 1;
                }
            }
            CellsHeaderExtractor extractor2 = extractNames(table2);

            String methodName1 = getMethodName(table1);
            if (StringUtils.isNotBlank(methodName1)) {
                if (extractor2.getDependentSpreadsheetTypes().contains(methodName1)) {
                    return -1;
                }
            }
            return 0;
        }
        return 0;
    }

    // TODO: refactor
    // extract working with header to helper class
    // should be simple: Helper.getMethodName(tableHeader)
    //
    private String getMethodName(TableSyntaxNode table) {
        String methodName = StringUtils.EMPTY;

        String[] tokens = getSignature(table).split(" ");
        if (tokens != null && tokens.length > 2) {
            int bracketIndex = tokens[2].indexOf("(");
            if (bracketIndex >= 0) {
                methodName = tokens[2].substring(0, bracketIndex);
            } else {
                methodName = tokens[2];
            }
        }
        return methodName;
    }

    private String getSignature(TableSyntaxNode table) {
        return table.getHeader().getHeaderToken().getModule().getCode();
    }

    private boolean isSpreadsheet(TableSyntaxNode o1) {
        return XlsNodeTypes.XLS_SPREADSHEET.equals(o1.getNodeType());
    }

    private CellsHeaderExtractor extractNames(TableSyntaxNode tableSyntaxNode) {
        CellsHeaderExtractor extractor = null;

        // try to get previously stored extractor
        //
        extractor = ((SpreadsheetHeaderNode) tableSyntaxNode.getHeader()).getCellHeadersExtractor();

        if (extractor == null) {
            extractor = new CellsHeaderExtractor(getSignature(tableSyntaxNode), tableSyntaxNode.getTableBody().getRow(0).getColumns(1),
                    tableSyntaxNode.getTableBody().getColumn(0).getRows(1));
            extractor.extract();

            // set cells header extractor to the table syntax node, to avoid
            // extracting several times
            //
            ((SpreadsheetHeaderNode) tableSyntaxNode.getHeader()).setCellHeadersExtractor(extractor);
        }

        return extractor;
    }

}
