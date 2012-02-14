package org.openl.rules.types.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.MethodUtil;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * OpenMethodDispatcher based on dispatcher table.
 * 
 * @author PUdalau
 */
public class OverloadedMethodsDispatcherTable extends MatchingOpenMethodDispatcher {

    private static final Log LOG = LogFactory.getLog(OverloadedMethodsDispatcherTable.class);

    public OverloadedMethodsDispatcherTable(IOpenMethod method, XlsModuleOpenClass moduleOpenClass) {
        super(method, moduleOpenClass);
    }

    private IOpenMethod dispatchingOpenMethod;

    public IOpenMethod getDispatchingOpenMethod() {
        return dispatchingOpenMethod;
    }

    public void setDispatchingOpenMethod(IOpenMethod dispatchingOpenMethod) {
        this.dispatchingOpenMethod = dispatchingOpenMethod;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (dispatchingOpenMethod != null) {
            return dispatchingOpenMethod.invoke(target, updateArguments(params, env, dispatchingOpenMethod), env);
        } else {
            LOG.warn(String.format("Dispatcher table for methods group [%s] was not built correctly. Dispatching will be passed through the java code instead of dispatcher table.",
                MethodUtil.printMethod(getName(), getSignature().getParameterTypes())));
            return invokeJavaDispatching(target, params, env);
        }
    }
    
    public Object invokeJavaDispatching(Object target, Object[] params, IRuntimeEnv env) {
        if (Tracer.isTracerOn()) {
            return invokeTraced(target, params, env);
        } else {
            return super.invoke(target, params, env);
        }
    }

    private Object[] updateArguments(Object[] params, IRuntimeEnv env, IOpenMethod dispatcherMethod) {
        Object[] arguments = new Object[dispatcherMethod.getSignature().getNumberOfParameters()];
        int parametersOfOverloadedMethods = getCandidates().get(0).getSignature().getNumberOfParameters();
        if (parametersOfOverloadedMethods > 0) {
            System.arraycopy(params, 0, arguments, 0, params.length);
        }
        IRulesRuntimeContext context = (IRulesRuntimeContext) env.getContext();
        if (context != null) {
            for (int i = parametersOfOverloadedMethods; i < dispatcherMethod.getSignature().getNumberOfParameters(); i++) {
                arguments[i] = context.getValue(dispatcherMethod.getSignature().getParameterName(i));
            }
        }
        return arguments;
    }

    public TableSyntaxNode getDispatcherTable() {
        if (dispatchingOpenMethod != null) {
            return (TableSyntaxNode) dispatchingOpenMethod.getInfo().getSyntaxNode();
        } else {
            return super.getDispatcherTable();
        }
    }
}
