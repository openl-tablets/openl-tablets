package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.vm.IRuntimeEnv;

/**
 * Defines bound node that cannot be bound successfully.
 *
 * @author snshor, Yury Molchan
 */
public class ErrorBoundNode extends ABoundNode {

    public ErrorBoundNode(ISyntaxNode node) {
        super(node);
    }

    /**
     * An error node keeping the nodes of the expression that were bound, so their hints and links stay available.
     */
    public ErrorBoundNode(ISyntaxNode node, IBoundNode... bound) {
        super(node, bound);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException("You are trying to run openl code with a compile error in it");
    }
}
