package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.syntax.impl.NaryNode;

public class WorkbookSyntaxNode extends NaryNode {

    private TableSyntaxNode[] mergedTableParts;

    public WorkbookSyntaxNode(WorksheetSyntaxNode[] nodes,
            TableSyntaxNode[] mergedTableParts,
            XlsWorkbookSourceCodeModule module) {
        super(XlsNodeTypes.XLS_WORKBOOK.toString(), null, nodes, module);
        this.mergedTableParts = mergedTableParts;
    }

    private TableSyntaxNode[] tableSyntaxNodes = null;

    public TableSyntaxNode[] getTableSyntaxNodes() {
        if (tableSyntaxNodes == null) {
            buildTableSyntaxNodes();
        } else {
            int expectedSize = 0;
            for (WorksheetSyntaxNode sheetNode : getWorksheetSyntaxNodes()) {
                expectedSize = expectedSize + sheetNode.getTableSyntaxNodes().length;
            }
            expectedSize = expectedSize + mergedTableParts.length;
            if (expectedSize != tableSyntaxNodes.length) {
                buildTableSyntaxNodes();
            }
        }
        return tableSyntaxNodes;
    }

    private void buildTableSyntaxNodes() {
        List<TableSyntaxNode> tnodes = new ArrayList<>();
        WorksheetSyntaxNode[] sheetNodes = getWorksheetSyntaxNodes();

        for (WorksheetSyntaxNode sheetNode : sheetNodes) {
            TableSyntaxNode[] tableSyntaxNodes = sheetNode.getTableSyntaxNodes();
            for (TableSyntaxNode tsn : tableSyntaxNodes) {
                tnodes.add(tsn);
            }
        }

        for (TableSyntaxNode tnode : mergedTableParts) {
            tnodes.add(tnode);
        }
        tableSyntaxNodes = tnodes.toArray(TableSyntaxNode.EMPTY_ARRAY);
    }

    public XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule() {
        return (XlsWorkbookSourceCodeModule) getModule();
    }

    public WorksheetSyntaxNode[] getWorksheetSyntaxNodes() {
        return (WorksheetSyntaxNode[]) getNodes();
    }

}
