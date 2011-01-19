/*
 * Created on Jun 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public abstract class ATargetBoundNode extends ABoundNode {

    protected IBoundNode targetNode;

    /**
     * @param syntaxNode
     * @param children
     */
    public ATargetBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    public ATargetBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, IBoundNode targetNode) {
        super(syntaxNode, children);
        this.targetNode = targetNode;
    }

    /**
     * @return
     */
    @Override
    public IBoundNode getTargetNode() {
        return targetNode;
    }

    /**
     * @param node
     */
    public void setTargetNode(IBoundNode node) {
        targetNode = node;
    }

}
