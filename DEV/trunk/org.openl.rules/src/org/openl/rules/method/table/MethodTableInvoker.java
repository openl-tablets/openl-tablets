package org.openl.rules.method.table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link TableMethod}. 
 * 
 * @author DLiauchuk
 *
 *
 */
public class MethodTableInvoker extends RulesMethodInvoker {

    private final Log LOG = LogFactory.getLog(MethodTableInvoker.class);

    public MethodTableInvoker(TableMethod tableMethod) {
        super(tableMethod);
    }
    
    @Override
    public TableMethod getInvokableMethod() {    
        return (TableMethod)super.getInvokableMethod();
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {        
        return getInvokableMethod().getCompositeMethod().invoke(target, params, env);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Tracer tracer = Tracer.getTracer();

        MethodTableTraceObject traceObject = (MethodTableTraceObject)getTraceObject(params);
        tracer.push(traceObject);

        Object result = null; 

        try {
            result = getInvokableMethod().getCompositeMethod().invoke(target, params, env);
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
        return getInvokableMethod().getCompositeMethod().getMethodBodyBoundNode() != null;
    }
   
}
