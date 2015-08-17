package org.openl.rules.lang.xls;

import java.util.LinkedList;
import java.util.Queue;

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
public class SpreadsheetNodeSorter {

    private static boolean check(TableSyntaxNode table1, TableSyntaxNode table2) {
        if (isSpreadsheet(table1) && isSpreadsheet(table2)) {
            // Compare both spreadsheets table syntax nodes.
            // Check if there are usages of custom spreadsheet type
            //
            CellsHeaderExtractor extractor1 = extractNames(table1);

            String methodName2 = getMethodName(table2);
            if (StringUtils.isNotBlank(methodName2)) {
                if (extractor1.getDependentSpreadsheetTypes().contains(methodName2)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static TableSyntaxNode[] sort(TableSyntaxNode[] tableSyntaxNodes){
        TableSyntaxNode[] result = new TableSyntaxNode[tableSyntaxNodes.length];
        boolean[][] matrix = new boolean[tableSyntaxNodes.length][tableSyntaxNodes.length];
        int[] c = new int[tableSyntaxNodes.length];
        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            for (int j = 0; j < tableSyntaxNodes.length; j++) {
                if (i != j && check(tableSyntaxNodes[i], tableSyntaxNodes[j])) {
                    matrix[j][i] = true;
                    c[i]++;
                }
            }
        }
        int n = 0;
        Queue<Integer> q = new LinkedList<Integer>();
        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            if (c[i] == 0) {
                q.add(i);
            }
        }
        while (!q.isEmpty()){
            int t = q.poll();
            result[n++] = tableSyntaxNodes[t];
            for (int i = 0;i<tableSyntaxNodes.length;i++){
                if (matrix[t][i]){
                    c[i]--;
                    if (c[i] == 0){
                        q.add(i);
                    }
                }
            }
        }
        if (n < tableSyntaxNodes.length){
            for (int i = 0; i < tableSyntaxNodes.length; i++) {
                if (c[i] > 0) {
                    result[n++] = tableSyntaxNodes[i];
                }
            }
        }
        return result;
    }

    // TODO: refactor
    // extract working with header to helper class
    // should be simple: Helper.getMethodName(tableHeader)
    //
    private static String getMethodName(TableSyntaxNode table) {
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

    private static String getSignature(TableSyntaxNode table) {
        return table.getHeader().getHeaderToken().getModule().getCode();
    }

    private static boolean isSpreadsheet(TableSyntaxNode o1) {
        return XlsNodeTypes.XLS_SPREADSHEET.equals(o1.getNodeType());
    }

    private static CellsHeaderExtractor extractNames(TableSyntaxNode tableSyntaxNode) {
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
