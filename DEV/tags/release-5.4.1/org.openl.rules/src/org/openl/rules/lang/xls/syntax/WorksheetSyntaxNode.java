package org.openl.rules.lang.xls.syntax;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.syntax.impl.NaryNode;

public class WorksheetSyntaxNode extends NaryNode implements ITableNodeTypes{

    public WorksheetSyntaxNode(TableSyntaxNode[] nodes, XlsSheetSourceCodeModule module) {
        super(XLS_WORKSHEET, null, nodes, module);
    }
    
    public TableSyntaxNode[] getTableSyntaxNodes()
    {
        return (TableSyntaxNode[]) nodes;
    }
    

}
