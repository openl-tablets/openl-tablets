package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class PrefixNode extends MethodBoundNode {
    /**
     * @param syntaxNode
     * @param child
     * @param method
     */
    public PrefixNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method) {
        super(syntaxNode, child, method);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object oldValue = children[0].evaluate(env);
        Object newValue = boundMethod.invoke(null, new Object[] { oldValue }, env);

        children[0].assign(newValue, env);

        return newValue;
    }

}
