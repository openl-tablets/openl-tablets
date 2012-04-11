/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.List;

import org.openl.IOpenSourceCodeModule;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.NaryNode;

/**
 * @author snshor
 *
 */
public class XlsModuleSyntaxNode extends NaryNode implements ITableNodeTypes {
	
	private List<IdentifierNode> extensionNodes;

    OpenlSyntaxNode openlNode;
    IdentifierNode vocabularyNode;
    String allImportString;

    public XlsModuleSyntaxNode(TableSyntaxNode[] nodes, IOpenSourceCodeModule module, OpenlSyntaxNode openlNode,
            IdentifierNode vocabularyNode, String allImportString, List<IdentifierNode> extensionNodes) {
        super(XLS_MODULE, null, nodes, module);

        this.openlNode = openlNode;
        this.vocabularyNode = vocabularyNode;
        this.allImportString = allImportString;
        this.extensionNodes = extensionNodes;
    }

    public String getAllImportString() {
        return allImportString;
    }

    public OpenlSyntaxNode getOpenlNode() {
        return openlNode;
    }

    public IdentifierNode getVocabularyNode() {
        return vocabularyNode;
    }

    public TableSyntaxNode[] getXlsTableSyntaxNodes() {
        return (TableSyntaxNode[]) nodes;
    }
    
    public List<IdentifierNode> getExtensionNodes() {
		return extensionNodes;
	}

	public TableSyntaxNode[] getXlsTableSyntaxNodesWithoutErrors() {
        List<TableSyntaxNode> resultNodes = new ArrayList<TableSyntaxNode>();
        if (nodes != null) {
            for (TableSyntaxNode node : (TableSyntaxNode[]) nodes) {
                ISyntaxError[] errors = node.getErrors();
                if (errors != null && errors.length > 0) {
                    continue;
                }
                resultNodes.add(node);
            }
        }
        return resultNodes.toArray(new TableSyntaxNode[0]);
    }

}
