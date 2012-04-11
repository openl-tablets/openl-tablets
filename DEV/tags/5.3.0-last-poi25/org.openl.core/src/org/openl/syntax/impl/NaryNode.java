/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 *
 */
public class NaryNode extends ASyntaxNode {

    protected ISyntaxNode[] nodes;

    public NaryNode(String type, TextInterval pos, ISyntaxNode[] nodes, IOpenSourceCodeModule module) {
        super(type, pos, module);
        this.nodes = nodes;
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].setParent(this);
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
