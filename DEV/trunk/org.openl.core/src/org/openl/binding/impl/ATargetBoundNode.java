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
     * @deprecated 22.02.2011. Is not used any more 
     */
    @Deprecated
    public ATargetBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        this(syntaxNode, children, null);
    }

    public ATargetBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, IBoundNode targetNode) {
        super(syntaxNode, children);
        this.targetNode = targetNode;
    }

    @Override
    public IBoundNode getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(IBoundNode node) {
        targetNode = node;
    }

}
