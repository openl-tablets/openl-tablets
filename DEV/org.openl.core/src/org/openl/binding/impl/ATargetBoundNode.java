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

    private IBoundNode targetNode;

    public ATargetBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, IBoundNode targetNode) {
        super(syntaxNode, children);
        this.targetNode = targetNode;
    }

    @Override
    public IBoundNode getTargetNode() {
        return targetNode;
    }
}
