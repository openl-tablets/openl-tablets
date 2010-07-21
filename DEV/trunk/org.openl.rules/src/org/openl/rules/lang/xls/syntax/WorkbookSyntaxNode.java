package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.syntax.impl.NaryNode;

public class WorkbookSyntaxNode extends NaryNode {

    public WorkbookSyntaxNode(WorksheetSyntaxNode[] nodes, XlsWorkbookSourceCodeModule module) {
        super(ITableNodeTypes.XLS_WORKBOOK, null, nodes, module);
    }
    
    public TableSyntaxNode[] getTableSyntaxNodes() {
        List<TableSyntaxNode> tnodes = new ArrayList<TableSyntaxNode>();               
        WorksheetSyntaxNode[] sheetNodes = getWorksheetSyntaxNodes();
        
        for (WorksheetSyntaxNode sheetNode :  sheetNodes) {
            TableSyntaxNode[] tableSyntaxNodes = sheetNode.getTableSyntaxNodes();
            for (TableSyntaxNode tsn : tableSyntaxNodes) {
                tnodes.add(tsn);
            }
        }        
        return tnodes.toArray(new TableSyntaxNode[0]);
    }
    
    public XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule() {
        return (XlsWorkbookSourceCodeModule) getModule();
    }
    
    public WorksheetSyntaxNode[] getWorksheetSyntaxNodes() {
        return (WorksheetSyntaxNode[])getNodes();
    }
}
