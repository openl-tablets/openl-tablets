package org.openl.rules.method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Default implementation for invokers supporting tracing.
 *
 * @author Yury Molchan
 */
public abstract class RulesMethodInvoker<T extends ExecutableRulesMethod> implements Invokable {

    private final T invokableMethod;

    protected RulesMethodInvoker(T invokableMethod) {
        this.invokableMethod = invokableMethod;
    }

    @Override
    public final Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // check if the object can be invoked
        if (!canInvoke()) {
            throw new OpenLRuntimeException("Method cannot be invoked");
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
    protected abstract boolean canInvoke();

    /**
     * Invoke for simple run operation.
     */
    protected abstract Object invokeSimple(Object target, Object[] params, IRuntimeEnv env);
}
