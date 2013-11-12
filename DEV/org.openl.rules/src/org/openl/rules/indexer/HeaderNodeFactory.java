package org.openl.rules.indexer;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

public class HeaderNodeFactory {
    private HeaderNodeFactory(){}
    
    public static HeaderSyntaxNode getHeaderNode(String xls_type, GridCellSourceCodeModule src, IdentifierNode parsedHeader) {
        if (XlsNodeTypes.XLS_SPREADSHEET.toString().equals(xls_type)) {
            return new SpreadsheetHeaderNode(src, parsedHeader); 
        } else {
            return new HeaderSyntaxNode(src, parsedHeader);
        }
        
    }
}
