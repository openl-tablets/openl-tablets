package org.openl.rules.lang.xls.syntax;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.syntax.impl.NaryNode;

public class WorksheetSyntaxNode extends NaryNode {

    public WorksheetSyntaxNode(TableSyntaxNode[] nodes, XlsSheetSourceCodeModule module) {
        super(XlsNodeTypes.XLS_WORKSHEET.toString(), null, nodes, module);
    }

    public TableSyntaxNode[] getTableSyntaxNodes() {
        return (TableSyntaxNode[]) getNodes();
    }

}
