/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
package org.openl.syntax.impl;

import org.apache.commons.lang3.ArrayUtils;
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

        for (ISyntaxNode node : this.nodes) {
            if (node != null) {
                node.setParent(this);
            }
        }
    }

    public ISyntaxNode[] getNodes() {
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getNumberOfChildren()
     */
    @Override
    public int getNumberOfChildren() {
        return nodes.length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getChild(int)
     */
    @Override
    public ISyntaxNode getChild(int i) {
        return nodes[i];
    }
    
    public void addNode(ISyntaxNode node) { 
        if (node != null) {          
            node.setParent(this);
            nodes = ArrayUtils.add(nodes, node);
        }
    }

}
