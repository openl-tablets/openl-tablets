/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class SuffixNode extends MethodBoundNode {
    /**
     * @param syntaxNode
     * @param child
     * @param method
     */
    public SuffixNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method) {
        super(syntaxNode, child, method);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    @Override
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {        
        Object oldValue = children[0].evaluate(env);
        Object newValue = boundMethod.invoke(null, new Object[] { oldValue }, env);

        children[0].assign(newValue, env);

        return oldValue;
    }

}
