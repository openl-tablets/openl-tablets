package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOwnTargetMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodBoundNode extends ATargetBoundNode {

    protected IMethodCaller boundMethod;

    public MethodBoundNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller methodCaller) {
        this(syntaxNode, child, methodCaller, null);
    }

    public MethodBoundNode(ISyntaxNode syntaxNode,
            IBoundNode[] child,
            IMethodCaller methodCaller,
            IBoundNode targetNode) {
        super(syntaxNode, targetNode, child);
        this.boundMethod = methodCaller;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        try {
            Object target = getTarget(env);
            Object[] pars = evaluateChildren(env);
            if (target == null && !(boundMethod instanceof IOwnTargetMethod) && !boundMethod.getMethod().isStatic()) {
                return getType().nullObject();
            } else {
                return boundMethod.invoke(target, pars, env);
            }
        } catch (ControlSignalReturn signal) {
            return signal.getReturnValue();
        } catch (OpenLRuntimeException opex) {
            opex.pushMethodNode(this);
            throw opex;
        }

    }

    @Override
    public IOpenClass getType() {
        return boundMethod.getMethod().getType();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addMethodDependency(boundMethod.getMethod(), this);
    }

    public IMethodCaller getMethodCaller() {
        return boundMethod;
    }
}
