package org.openl.rules.cmatch;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.method.DefaultInvokerWithTrace;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.rules.method.TracedObjectFactory;
import org.openl.rules.table.ATableTracerNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class ColumnMatchInvoker extends RulesMethodInvoker {

//    private ColumnMatch columnMatch;

    public ColumnMatchInvoker(ColumnMatch columnMatch) {
        super(columnMatch);
//        this.columnMatch = columnMatch;
    }
    
    @Override
    public ColumnMatch getInvokableMethod() {
        return (ColumnMatch)super.getInvokableMethod();
    }

    public boolean canInvoke() {        
        return getInvokableMethod().getAlgorithmExecutor() != null;
    }

//    public ATableTracerNode createTraceObject(Object[] params) {        
////        return new ColumnMatchTraceObject(columnMatch, params);
//        return TracedObjectFactory.getTracedObject(columnMatch, params);
//    }

//    protected OpenLRuntimeException getError() {        
//        return new OpenLRuntimeException(getInvokableMethod().getSyntaxNode().getErrors()[0]);
//    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        Object result = getInvokableMethod().getAlgorithmExecutor().invoke(target, params, env, getInvokableMethod());

        if (result == null) {
            IOpenClass type = getInvokableMethod().getHeader().getType();
            if (type.getInstanceClass().isPrimitive()) {
                throw new IllegalArgumentException("Cannot return <null> for primitive type " + type.getInstanceClass()
                    .getName());
            }
        }

        return result;        
    }

    /**
     * Column match is traceable. But trace is handled inside algorithm executor. See implementations 
     * of {@link IMatchAlgorithmExecutor#invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch)}
     * 
     */
    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        // trace operations implemented in 
        // IMatchAlgorithmExecutor#invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch)}
        //
        return null;
    }

    /**
     * Overrides {@link DefaultInvokerWithTrace#invoke()} as trace operations are implemented on other level.
     * See {@link #invokeTraced()}
     */
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (canInvoke()) {
            return invokeSimple(target, params, env);
        } else {
            OpenLRuntimeException error = getError();
            if (isTracerOn()) {
                setErrorToTrace(error, params);
            }
            throw error;
        }
    }
}
