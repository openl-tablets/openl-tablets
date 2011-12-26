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
    public static final String DISPATCHING_MODE_DT = "dt";

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

    public boolean isJavaDispatchingMode() {
        String dispatchingMode = System.getProperty(DISPATCHING_MODE_PROPERTY);
        return dispatchingMode != null && dispatchingMode.equalsIgnoreCase(DISPATCHING_MODE_JAVA);
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
        System.arraycopy(params, 0, arguments, 0, params.length);
        IRulesRuntimeContext context = (IRulesRuntimeContext) env.getContext();
        if (context != null) {
            for (int i = params.length; i < dispatcherMethod.getSignature().getNumberOfParameters(); i++) {
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
