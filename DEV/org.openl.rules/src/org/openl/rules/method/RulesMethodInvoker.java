package org.openl.rules.method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Default implementation for invokers supporting tracing.
 *
 * @author Yury Molchan
 */
public abstract class RulesMethodInvoker<T extends ExecutableRulesMethod> implements Invokable {

    private T invokableMethod;

    protected RulesMethodInvoker(T invokableMethod) {
        this.invokableMethod = invokableMethod;
    }

    public final Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // check if the object can be invoked
        if (!canInvoke()) {
            // object can`t be invoked, inform user about the problem.
            SyntaxNodeException cause = getInvokableMethod().getSyntaxNode().getErrors()[0];
            throw new OpenLRuntimeException(cause);
        } else {
            // simple run invoke
            return invokeSimple(target, params, env);
        }
    }

    public T getInvokableMethod() {
        return invokableMethod;
    }

    /**
     * Checks if it is possible to invoke invokable object.
     */
    abstract protected boolean canInvoke();

    /**
     * Invoke for simple run operation.
     */
    abstract protected Object invokeSimple(Object target, Object[] params, IRuntimeEnv env);
}
