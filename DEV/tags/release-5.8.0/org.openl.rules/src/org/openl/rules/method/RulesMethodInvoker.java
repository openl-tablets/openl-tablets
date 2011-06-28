package org.openl.rules.method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.ATableTracerNode;

/**
 * Default implementation for {@link DefaultInvokerWithTrace}. {@link ExecutableRulesMethod}
 * is used as traced object.
 * 
 * @author DLiauchuk
 *
 */
public abstract class RulesMethodInvoker extends DefaultInvokerWithTrace {
    
    private ExecutableRulesMethod invokableMethod;
    
    public RulesMethodInvoker(ExecutableRulesMethod invokableMethod) {
        this.invokableMethod = invokableMethod;
    }

    public ExecutableRulesMethod getInvokableMethod() {
        return invokableMethod;
    }

    @Override
    protected ATableTracerNode getTraceObject(Object[] params) {
        return TracedObjectFactory.getTracedObject(invokableMethod, params);        
    }
    
    @Override
    protected OpenLRuntimeException getError() {
        return new OpenLRuntimeException(getInvokableMethod().getSyntaxNode().getErrors()[0]);
    }
}
