package org.openl.rules.types.impl;

import org.openl.binding.MethodUtil;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenMethodDispatcher based on dispatcher table.
 *
 * @author PUdalau
 */
public class OverloadedMethodsDispatcherTable extends MatchingOpenMethodDispatcher {

    private final Logger log = LoggerFactory.getLogger(OverloadedMethodsDispatcherTable.class);

    public OverloadedMethodsDispatcherTable() { //For CGLIB proxing
    }

    public OverloadedMethodsDispatcherTable(IOpenMethod method, XlsModuleOpenClass moduleOpenClass) {
        super(method, moduleOpenClass);
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (getDispatchingOpenMethod() != null) {
            return getDispatchingOpenMethod().invoke(target, updateArguments(params, env, getDispatchingOpenMethod()), env);
        } else {
            log.warn("Dispatcher table for methods group [{}] wasn't built correctly. Dispatching will be passed through the java code instead of dispatcher table.",
                    MethodUtil.printMethod(getName(), getSignature().getParameterTypes()));
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
        if (getDispatchingOpenMethod() != null) {
            return (TableSyntaxNode) getDispatchingOpenMethod().getInfo().getSyntaxNode();
        } else {
            return super.getDispatcherTable();
        }
    }
}
