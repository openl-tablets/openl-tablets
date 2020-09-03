package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor, Yury Molchan
 *
 */
public abstract class ATargetBoundNode extends ABoundNode {

    private final IBoundNode targetNode;

    public ATargetBoundNode(ISyntaxNode syntaxNode, IBoundNode targetNode, IBoundNode... children) {
        super(syntaxNode, children);
        this.targetNode = targetNode;
    }

    protected Object getTarget(IRuntimeEnv env) {
        return getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);
    }

    @Override
    public IBoundNode getTargetNode() {
        return targetNode;
    }
}
