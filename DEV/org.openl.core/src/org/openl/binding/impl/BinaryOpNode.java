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
    private IBoundNode left, right;

    public BinaryOpNode(ISyntaxNode syntaxNode, IBoundNode left, IBoundNode right, IMethodCaller method) {
        super(syntaxNode, method, left, right);
        this.left = left;
        this.right = right;
        useBinaryMethod = method.getMethod().getSignature().getParameterTypes().length == 2;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object leftValue = left.evaluate(env);
        Object rightValue = right.evaluate(env);

        if (useBinaryMethod) {
            return boundMethod.invoke(null, new Object[] { leftValue, rightValue }, env);
        }
        return boundMethod.invoke(leftValue, new Object[] { rightValue }, env);

    }
}
