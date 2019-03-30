package org.openl.binding.impl;

import org.openl.syntax.ISyntaxNode;
import org.openl.vm.IRuntimeEnv;

/**
 * Defines bound node that cannot be bound successfully.
 * 
 * @author snshor, Yury Molchan
 * 
 */
public class ErrorBoundNode extends ABoundNode {

    public ErrorBoundNode(ISyntaxNode node) {
        super(node);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException("You are trying to run openl code with a compile error in it");
    }
}
