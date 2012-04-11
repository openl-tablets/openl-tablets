package org.openl.rules.cmatch;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.cmatch.algorithm.ColumnMatchTraceObject;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.DefaultInvokerWithTrace;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ColumnMatchInvoker extends DefaultInvokerWithTrace {

    private ColumnMatch columnMatch;

    public ColumnMatchInvoker(ColumnMatch columnMatch) {        
        this.columnMatch = columnMatch;
    }

    public boolean canInvoke() {        
        return columnMatch.getAlgorithmExecutor() != null;
    }

    public ATableTracerNode createTraceObject(Object[] params) {        
        return new ColumnMatchTraceObject(columnMatch, params);
    }

    public OpenLRuntimeException getError() {        
        return new OpenLRuntimeException(columnMatch.getSyntaxNode().getErrors()[0]);
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        Object result = columnMatch.getAlgorithmExecutor().invoke(target, params, env, columnMatch);

        if (result == null) {
            IOpenClass type = columnMatch.getHeader().getType();
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
