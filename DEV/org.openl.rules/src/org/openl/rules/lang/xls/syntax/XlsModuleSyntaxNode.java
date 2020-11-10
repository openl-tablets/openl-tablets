/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.syntax;

import java.util.*;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.NaryNode;

/**
 * @author snshor
 *
 */
public class XlsModuleSyntaxNode extends NaryNode {

    private final OpenlSyntaxNode openlNode;

    private final Set<String> imports;

    public XlsModuleSyntaxNode(WorkbookSyntaxNode[] nodes,
            IOpenSourceCodeModule module,
            OpenlSyntaxNode openlNode,
            Collection<String> imports) {
        super(XlsNodeTypes.XLS_MODULE.toString(), null, nodes, module);

        this.openlNode = openlNode;
        this.imports = new HashSet<>(imports);
    }

    public Collection<String> getImports() {
        return Collections.unmodifiableSet(imports);
    }

    public void addImport(String value) {
        imports.add(value);
    }

    public OpenlSyntaxNode getOpenlNode() {
        return openlNode;
    }

    public WorkbookSyntaxNode[] getWorkbookSyntaxNodes() {
        return (WorkbookSyntaxNode[]) getNodes();
    }

    private TableSyntaxNode[] tableSyntaxNodes;

    public TableSyntaxNode[] getXlsTableSyntaxNodes() {
        if (tableSyntaxNodes == null) {
            buildXlsTableSyntaxNodes();
        } else {
            int expectedSize = 0;
            for (WorkbookSyntaxNode wbsn : getWorkbookSyntaxNodes()) {
                expectedSize = expectedSize + wbsn.getTableSyntaxNodes().length;
            }
            if (expectedSize != tableSyntaxNodes.length) {
                buildXlsTableSyntaxNodes();
            }
        }
        return tableSyntaxNodes;
    }

    private void buildXlsTableSyntaxNodes() {
        List<TableSyntaxNode> tsnodes = new ArrayList<>();
        for (WorkbookSyntaxNode wbsn : getWorkbookSyntaxNodes()) {
            Collections.addAll(tsnodes, wbsn.getTableSyntaxNodes());
        }
        tableSyntaxNodes = tsnodes.toArray(TableSyntaxNode.EMPTY_ARRAY);
    }

    public TableSyntaxNode[] getXlsTableSyntaxNodesWithoutErrors() {
        List<TableSyntaxNode> resultNodes = new ArrayList<>();
        for (TableSyntaxNode node : getXlsTableSyntaxNodes()) {
            if (node.hasErrors()) {
                continue;
            }
            resultNodes.add(node);
        }
        return resultNodes.toArray(TableSyntaxNode.EMPTY_ARRAY);
    }

}
