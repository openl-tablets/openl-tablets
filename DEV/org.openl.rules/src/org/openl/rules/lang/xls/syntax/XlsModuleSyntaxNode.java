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
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.NaryNode;

/**
 * @author snshor
 * 
 */
public class XlsModuleSyntaxNode extends NaryNode {

    private OpenlSyntaxNode openlNode;

    private IdentifierNode vocabularyNode;

    private Set<String> imports = new HashSet<String>();

    public XlsModuleSyntaxNode(WorkbookSyntaxNode[] nodes,
            IOpenSourceCodeModule module,
            OpenlSyntaxNode openlNode,
            IdentifierNode vocabularyNode,
            Collection<String> imports) {
        super(XlsNodeTypes.XLS_MODULE.toString(), null, nodes, module);

        this.openlNode = openlNode;
        this.vocabularyNode = vocabularyNode;
        this.imports.addAll(imports);
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

    public IdentifierNode getVocabularyNode() {
        return vocabularyNode;
    }

    public WorkbookSyntaxNode[] getWorkbookSyntaxNodes() {
        return (WorkbookSyntaxNode[]) getNodes();
    }

    public TableSyntaxNode[] getXlsTableSyntaxNodes() {

        List<TableSyntaxNode> tsnodes = new ArrayList<TableSyntaxNode>();

        for (WorkbookSyntaxNode wbsn : getWorkbookSyntaxNodes()) {
            for (TableSyntaxNode tableSyntaxNode : wbsn.getTableSyntaxNodes()) {
                tsnodes.add(tableSyntaxNode);
            }
        }
        return tsnodes.toArray(new TableSyntaxNode[tsnodes.size()]);
    }

    public TableSyntaxNode[] getXlsTableSyntaxNodesWithoutErrors() {
        List<TableSyntaxNode> resultNodes = new ArrayList<TableSyntaxNode>();
        for (TableSyntaxNode node : getXlsTableSyntaxNodes()) {
            if (node.hasErrors()) {
                continue;
            }
            resultNodes.add(node);
        }
        return resultNodes.toArray(new TableSyntaxNode[resultNodes.size()]);
    }

}
