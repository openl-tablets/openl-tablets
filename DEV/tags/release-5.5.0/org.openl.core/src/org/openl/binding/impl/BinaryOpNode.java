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
public class BinaryOpNode extends MethodBoundNode {
    static public Object evaluateBinaryMethod(IRuntimeEnv env, Object[] pars, IMethodCaller boundMethod)
            throws OpenLRuntimeException {

        if (boundMethod.getMethod().getSignature().getParameterTypes().length == 2) {
            return boundMethod.invoke(null, pars, env);
        }
        return boundMethod.invoke(pars[0], new Object[] { pars[1] }, env);

    }

    /**
     * @param syntaxNode
     * @param child
     * @param method
     */
    public BinaryOpNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method) {
        super(syntaxNode, child, method);
    }

    @Override
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object[] pars = evaluateChildren(env);

        return evaluateBinaryMethod(env, pars, boundMethod);

    }

}
