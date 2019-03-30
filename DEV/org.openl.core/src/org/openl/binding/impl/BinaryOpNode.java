package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class BinaryOpNode extends MethodBoundNode {
    public static Object evaluateBinaryMethod(IRuntimeEnv env, Object[] pars, IMethodCaller boundMethod) {

        if (boundMethod.getMethod().getSignature().getParameterTypes().length == 2) {
            return boundMethod.invoke(null, pars, env);
        }
        return boundMethod.invoke(pars[0], new Object[] { pars[1] }, env);

    }

    private boolean useBinaryMethod;

    /**
     * @param syntaxNode
     * @param child
     * @param method
     */
    public BinaryOpNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method) {
        super(syntaxNode, child, method);
        useBinaryMethod = method.getMethod().getSignature().getParameterTypes().length == 2;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object[] pars = evaluateChildren(env);

        if (useBinaryMethod) {
            return boundMethod.invoke(null, pars, env);
        }
        return boundMethod.invoke(pars[0], new Object[] { pars[1] }, env);

    }
}
