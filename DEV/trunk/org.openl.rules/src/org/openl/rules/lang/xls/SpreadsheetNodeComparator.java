package org.openl.rules.lang.xls;

import java.util.Comparator;

import org.openl.rules.calc.CellsHeaderExtractor;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Compares spreadsheets.
 * Spreadsheets with dependencies in cells on other Custom Spreadsheet results are considered to be greater.
 * 
 * @author DLiauchuk
 *
 */
public class SpreadsheetNodeComparator implements Comparator<TableSyntaxNode>{

    public int compare(TableSyntaxNode o1, TableSyntaxNode o2) {
        if (isSpreadsheet(o1) && isSpreadsheet(o2)) {
            // compare both spreadsheets table syntax nodes
            // check if there are usages of custom spreadsheet type
            //
            CellsHeaderExtractor extractor1 = extractNames(o1);            
            
            // TODO: refactor
            // extract working with header to helper class
            // should be simple: Helper.getMethodName(tableHeader)
            //
            String[] tokens = o2.getHeader().getHeaderToken().getModule().getCode().split(" ");
            if (tokens != null && tokens.length > 2) {
                String methodName = tokens[2].substring(0, tokens[2].indexOf("("));
                
                return extractor1.getDependentSpreadsheetTypes().contains(methodName) ? 1 : 0;
            } else {
                return 0;
            }
                    
        } else if (isSpreadsheet(o1)) {
            CellsHeaderExtractor extractor1 = extractNames(o1);
            return extractor1.getDependentSpreadsheetTypes().size() > 0 ? 1 : 0;        
        } else if (isSpreadsheet(o2)) {
            CellsHeaderExtractor extractor2 = extractNames(o2);
            return extractor2.getDependentSpreadsheetTypes().size() > 0 ? 0 : 1;
        } 
        return 0;
    }

    private boolean isSpreadsheet(TableSyntaxNode o1) {
        return XlsNodeTypes.XLS_SPREADSHEET.equals(o1.getNodeType());
    }

    private CellsHeaderExtractor extractNames(TableSyntaxNode tableSyntaxNode) {
        CellsHeaderExtractor extractor = null;
        
        // try to get previously stored extractor
        //
        extractor = ((SpreadsheetHeaderNode)tableSyntaxNode.getHeader()).getCellHeadersExtractor();
        
        if (extractor == null) {
            extractor = new CellsHeaderExtractor(tableSyntaxNode.getTableBody().getRow(0).getColumns(1), 
                tableSyntaxNode.getTableBody().getColumn(0).getRows(1));
            extractor.extract();    
            
            // set cells header extractor to the table syntax node, to avoid 
            // extracting several times
            //
            ((SpreadsheetHeaderNode)tableSyntaxNode.getHeader()).setCellHeadersExtractor(extractor);
       }
        
        return extractor;
    }

}
