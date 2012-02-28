/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
package org.openl.syntax.impl;

import org.apache.commons.lang.ArrayUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class NaryNode extends ASyntaxNode {

    private ISyntaxNode[] nodes;

    public NaryNode(String type, ILocation pos, ISyntaxNode[] nodes, IOpenSourceCodeModule module) {
        
        super(type, pos, module);
        
        this.nodes = nodes == null ? EMPTY : nodes;
        
        for (int i = 0; i < this.nodes.length; i++) {
            if (this.nodes[i] != null) {
                this.nodes[i].setParent(this);
            }
        }
    }

    public ISyntaxNode[] getNodes() {
        return nodes;
    }
    
    public void setNodes(ISyntaxNode[] nodes) {
        this.nodes = nodes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getNumberOfChildren()
     */
    public int getNumberOfChildren() {
        return nodes.length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getChild(int)
     */
    public ISyntaxNode getChild(int i) {
        return nodes[i];
    }
    
    public void addNode(ISyntaxNode node) { 
        if (node != null) {          
            node.setParent(this);
            ISyntaxNode[] newNodes = (ISyntaxNode[])ArrayUtils.add(nodes, node);
            nodes = newNodes;
        }
    }

}
