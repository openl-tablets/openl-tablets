package org.openl.rules.cmatch;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.cmatch.algorithm.ColumnMatchTraceObject;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.DefaultInvokerWithTrace;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ColumnMatchInvoker extends DefaultInvokerWithTrace {

    private ColumnMatch columnMatch;

    public ColumnMatchInvoker(ColumnMatch columnMatch, Object target, Object[] params, IRuntimeEnv env) {
        super(target, params, env);
        this.columnMatch = columnMatch;
    }

    public boolean canInvoke() {        
        return columnMatch.getAlgorithmExecutor() != null;
    }

    public ATableTracerNode createTraceObject() {        
        return new ColumnMatchTraceObject(columnMatch, getParams());
    }

    public OpenLRuntimeException getError() {        
        return new OpenLRuntimeException(columnMatch.getSyntaxNode().getErrors()[0]);
    }

    public Object invokeSimple() {
        Object result = columnMatch.getAlgorithmExecutor().invoke(getTarget(), getParams(), getEnv(), columnMatch);

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
     */
    public Object invokeTraced() {
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
    public Object invoke() {
        if (canInvoke()) {
            return invokeSimple();
        } else {
            OpenLRuntimeException error = getError();
            if (isTracerOn()) {
                setErrorToTrace(error);
            }
            throw error;
        }
    }
}
