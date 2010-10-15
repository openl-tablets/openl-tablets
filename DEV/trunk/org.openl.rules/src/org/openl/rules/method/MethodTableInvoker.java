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
 *
 */
public class MethodTableInvoker extends CompositeMethodInvoker implements InvokerWithTrace {

    private final Log LOG = LogFactory.getLog(MethodTableInvoker.class);

    private TableMethod tableMethod;

    public MethodTableInvoker(TableMethod tableMethod, Object target, Object[] params, IRuntimeEnv env) {
        super(tableMethod.getMethodBodyBoundNode(), target, params, env);
        this.tableMethod = tableMethod;
    }

    public Object invokeSimple() {        
        return super.invoke();
    }

    public Object invokeTraced() {
        Tracer tracer = Tracer.getTracer();

        MethodTableTraceObject traceObject = createTraceObject();
        tracer.push(traceObject);

        Object result = null; 

        try {
            result = super.invoke();
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

    public Object invoke() {
        // check if the object can be invoked
        //
        if (canInvoke()) {
            if (Tracer.isTracerOn()) {
                // invoke in trace
                return invokeTraced();
            } else {
                // simple run invoke
                return super.invoke();
            }
        } else {
            // object can`t be invoked, inform user about the problem.
            OpenLRuntimeException error = getError();
            if (Tracer.isTracerOn()) {
                setErrorToTrace(error);
            } 
            throw error;            
        }     
    }

    public boolean canInvoke() {        
        return getMethodBodyBoundNode() != null;
    }

    public MethodTableTraceObject createTraceObject() {        
        return new MethodTableTraceObject(tableMethod, getParams());
    }

    public OpenLRuntimeException getError() {        
        return new OpenLRuntimeException(tableMethod.getSyntaxNode().getErrors()[0]);
    }

    public void setErrorToTrace(OpenLRuntimeException error) {
        Tracer tracer = Tracer.getTracer();    
        ATableTracerNode traceObject = createTraceObject();
        traceObject.setError(error);
        tracer.push(traceObject);
        tracer.pop();
    }    
}
