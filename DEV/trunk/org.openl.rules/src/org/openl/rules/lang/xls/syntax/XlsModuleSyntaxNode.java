/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.NaryNode;

/**
 * @author snshor
 *
 */
public class XlsModuleSyntaxNode extends NaryNode  {
	
	private List<IdentifierNode> extensionNodes;

    private OpenlSyntaxNode openlNode;
    
    private IdentifierNode vocabularyNode;
    
    private List<String> allImports = new ArrayList<String>();
//    private List<String> modules = new ArrayList<String>();

    public XlsModuleSyntaxNode(WorkbookSyntaxNode[] nodes, IOpenSourceCodeModule module, OpenlSyntaxNode openlNode,
            IdentifierNode vocabularyNode, List<String> allImports, List<IdentifierNode> extensionNodes) {
        super(ITableNodeTypes.XLS_MODULE, null, nodes, module);

        this.openlNode = openlNode;
        this.vocabularyNode = vocabularyNode;
        this.allImports = allImports;
        this.extensionNodes = extensionNodes;
    }
    
//    public XlsModuleSyntaxNode(WorkbookSyntaxNode[] nodes, IOpenSourceCodeModule module, OpenlSyntaxNode openlNode,
//            IdentifierNode vocabularyNode, List<String> allImports, List<IdentifierNode> extensionNodes, List<String> modules) {
//        super(ITableNodeTypes.XLS_MODULE, null, nodes, module);
//
//        this.openlNode = openlNode;
//        this.vocabularyNode = vocabularyNode;
//        this.allImports = allImports;
//        this.extensionNodes = extensionNodes;
//        this.modules = modules;
//    }

    public List<String> getAllImports() {
        return allImports;
    }
    
//    public List<String> getImportedModules() {
//        return modules;
//    }

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
    
    public List<IdentifierNode> getExtensionNodes() {
		return extensionNodes;
	}

	public TableSyntaxNode[] getXlsTableSyntaxNodesWithoutErrors() {
        List<TableSyntaxNode> resultNodes = new ArrayList<TableSyntaxNode>();
            for (TableSyntaxNode node : getXlsTableSyntaxNodes()) {
                SyntaxNodeException[] errors = node.getErrors();
                if (errors != null && errors.length > 0) {
                    continue;
                }
                resultNodes.add(node);
            }
        return resultNodes.toArray(new TableSyntaxNode[resultNodes.size()]);
    }

}
