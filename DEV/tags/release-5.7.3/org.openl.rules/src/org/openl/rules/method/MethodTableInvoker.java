package org.openl.rules.method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.DefaultInvokerWithTrace;
import org.openl.rules.table.InvokerWithTrace;
import org.openl.types.impl.CompositeMethodInvoker;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link TableMethod}. 
 * 
 * @author DLiauchuk
 * 
 * TODO: refactor, it should extend {@link DefaultInvokerWithTrace}, as it have the same implementation functionality.
 * As this invoker extends CompositeMethodInvoker, there was no possibility to extend {@link DefaultInvokerWithTrace}.
 *
 */
public class MethodTableInvoker extends CompositeMethodInvoker implements InvokerWithTrace {

    private final Log LOG = LogFactory.getLog(MethodTableInvoker.class);

    private TableMethod tableMethod;

    public MethodTableInvoker(TableMethod tableMethod) {        
        super(tableMethod.getMethodBodyBoundNode());
        this.tableMethod = tableMethod;
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {        
        return super.invoke(target, params, env);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Tracer tracer = Tracer.getTracer();

        MethodTableTraceObject traceObject = createTraceObject(params);
        tracer.push(traceObject);

        Object result = null; 

        try {
            result = super.invoke(target, params, env);
            traceObject.setResult(result);
            return result;

        } catch (RuntimeException e) {
            traceObject.setError(e);
            LOG.error("Error when tracing Method table", e);
            throw e;
        } finally {
            tracer.pop();
        }
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // check if the object can be invoked
        //
        if (canInvoke()) {
            if (Tracer.isTracerOn()) {
                // invoke in trace
                return invokeTraced(target, params, env);
            } else {
                // simple run invoke
                return super.invoke(target, params, env);
            }
        } else {
            // object can`t be invoked, inform user about the problem.
            OpenLRuntimeException error = getError();
            if (Tracer.isTracerOn()) {
                setErrorToTrace(error, params);
            } 
            throw error;            
        }     
    }

    public boolean canInvoke() {        
        return getMethodBodyBoundNode() != null;
    }

    public MethodTableTraceObject createTraceObject(Object[] params) {        
        return new MethodTableTraceObject(tableMethod, params);
    }

    public OpenLRuntimeException getError() {        
        return new OpenLRuntimeException(tableMethod.getSyntaxNode().getErrors()[0]);
    }

    public void setErrorToTrace(OpenLRuntimeException error, Object[] params) {
        Tracer tracer = Tracer.getTracer();    
        ATableTracerNode traceObject = createTraceObject(params);
        traceObject.setError(error);
        tracer.push(traceObject);
        tracer.pop();
    }    
}
