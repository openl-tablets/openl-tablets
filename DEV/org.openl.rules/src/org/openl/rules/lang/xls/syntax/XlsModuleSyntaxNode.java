/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.NaryNode;

/**
 * @author snshor
 * 
 */
public class XlsModuleSyntaxNode extends NaryNode {

    private OpenlSyntaxNode openlNode;

    private Set<String> imports = new HashSet<>();
    private Set<String> libraries = new HashSet<>();

    public XlsModuleSyntaxNode(WorkbookSyntaxNode[] nodes,
            IOpenSourceCodeModule module,
            OpenlSyntaxNode openlNode,
            Collection<String> imports) {
        super(XlsNodeTypes.XLS_MODULE.toString(), null, nodes, module);

        this.openlNode = openlNode;
        this.imports.addAll(imports);
        this.libraries.addAll(libraries);
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

    private TableSyntaxNode[] tableSyntaxNodes = null;

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
            for (TableSyntaxNode tableSyntaxNode : wbsn.getTableSyntaxNodes()) {
                tsnodes.add(tableSyntaxNode);
            }
        }
        tableSyntaxNodes = tsnodes.toArray(new TableSyntaxNode[tsnodes.size()]);
    }

    public TableSyntaxNode[] getXlsTableSyntaxNodesWithoutErrors() {
        List<TableSyntaxNode> resultNodes = new ArrayList<>();
        for (TableSyntaxNode node : getXlsTableSyntaxNodes()) {
            if (node.hasErrors()) {
                continue;
            }
            resultNodes.add(node);
        }
        return resultNodes.toArray(new TableSyntaxNode[resultNodes.size()]);
    }

}
