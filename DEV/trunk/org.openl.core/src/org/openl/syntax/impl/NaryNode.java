/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class NaryNode extends ASyntaxNode {

    protected ISyntaxNode[] nodes;

    public NaryNode(String type, ILocation pos, ISyntaxNode[] nodes, IOpenSourceCodeModule module) {
        super(type, pos, module);
        this.nodes = nodes == null ? EMPTY : nodes;
        for (int i = 0; i < this.nodes.length; i++) {
            if (this.nodes[i] != null) {
                this.nodes[i].setParent(this);
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getChild(int)
     */
    public ISyntaxNode getChild(int i) {
        return nodes[i];
    }

    public ISyntaxNode[] getNodes() {
        return nodes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.parser.SyntaxNode#getNumberOfChildren()
     */
    public int getNumberOfChildren() {
        return nodes.length;
    }

    public void setNodes(ISyntaxNode[] nodes) {
        this.nodes = nodes;
    }

}
