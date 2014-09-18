package org.openl.rules.method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.ATableTracerNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Default implementation for invokers supporting tracing.
 *
 * @author DLiauchuk
 *
 */
public abstract class RulesMethodInvoker implements InvokerWithTrace {

    private ExecutableRulesMethod invokableMethod;

    protected RulesMethodInvoker(ExecutableRulesMethod invokableMethod) {
        this.invokableMethod = invokableMethod;
    }

    public final Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // check if the object can be invoked
        //
        if (canInvoke()) {
            if (Tracer.isTracerOn()) {
                // invoke in trace
                return invokeTraced(target, params, env);
            } else {
                // simple run invoke
                return invokeSimple(target, params, env);
            }
        } else {
            // object can`t be invoked, inform user about the problem.
            SyntaxNodeException cause = getInvokableMethod().getSyntaxNode().getErrors()[0];
            OpenLRuntimeException error = new OpenLRuntimeException(cause);
            if (Tracer.isTracerOn()) {
                setErrorToTrace(error, params);
            }
            throw error;
        }
    }


    /**
     * Creates traceable node for current invokable object.
     *
     * @param params
     * @return {@link ATableTracerNode} for current invokable object.
     */
    protected ATableTracerNode getTraceObject(Object[] params) {
        return TracedObjectFactory.getTracedObject(invokableMethod, params);
    }

    protected void setErrorToTrace(OpenLRuntimeException error, Object[] params) {
        ATableTracerNode traceObject = getTraceObject(params);
        traceObject.setError(error);
        Tracer.put(traceObject);
    }

    public ExecutableRulesMethod getInvokableMethod() {
        return invokableMethod;
    }
}
