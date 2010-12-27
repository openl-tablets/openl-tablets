package org.openl.rules.method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.DefaultInvokerWithTrace;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link TableMethod}. 
 * 
 * @author DLiauchuk
 *
 *
 */
public class MethodTableInvoker extends DefaultInvokerWithTrace {

    private final Log LOG = LogFactory.getLog(MethodTableInvoker.class);

    private TableMethod tableMethod;

    public MethodTableInvoker(TableMethod tableMethod) {
        this.tableMethod = tableMethod;
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {        
        return tableMethod.getCompositeMethod().invoke(target, params, env);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Tracer tracer = Tracer.getTracer();

        MethodTableTraceObject traceObject = createTraceObject(params);
        tracer.push(traceObject);

        Object result = null; 

        try {
            result = tableMethod.getCompositeMethod().invoke(target, params, env);
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

    public boolean canInvoke() {        
        return tableMethod.getCompositeMethod() != null;
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
