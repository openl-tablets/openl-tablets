package org.openl.rules.activiti;

import java.util.Collection;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.el.Expression;
import org.openl.CompiledOpenClass;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.activiti.util.IRulesRuntimeContextUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class MethodInvokeResourceServiceTask extends AbstractOpenLResourceServiceTask<Object> {
    protected Expression method;
    protected Expression resultVariable;

    private Object compiledInstance;
    private ObjectToDataOpenCastConvertor convertor = new ObjectToDataOpenCastConvertor();

    @Override
    protected IRulesRuntimeContext buildRuntimeContext(DelegateExecution execution) {
        Object runtimeContext = execution.getVariable("runtimeContext");
        if (runtimeContext instanceof IRulesRuntimeContext) {
            return (IRulesRuntimeContext) runtimeContext;
        }

        return IRulesRuntimeContextUtils.buildRuntimeContext(execution);
    }

    protected Object invokeOpenMethod(DelegateExecution execution) throws Exception {
        String methodValue = (String) method.getValue(execution);
        CompiledOpenClass compiledOpenClass = getSimpleProjectEngineFactory(execution).getCompiledOpenClass();
        Collection<IOpenMethod> methods = compiledOpenClass.getOpenClass().getMethods();

        IOpenMethod openMethod = null;
        int methodCount = 0;
        for (IOpenMethod m : methods) {
            if (methodValue.equals(m.getName())) {
                methodCount++;
                openMethod = m;
            }
        }

        if (methodCount == 0) {
            throw new MethodNotFoundException(String.format("Method '%s' is not found", methodValue));
        }

        if (methodCount > 1) {
            throw new MultipleMethodsFoundException(
                String.format("Found multiple methods with name '%s'", methodValue));
        }

        int n = openMethod.getSignature().getNumberOfParameters();
        IOpenCast[] openCasts = new IOpenCast[n];
        boolean f = true;
        for (int i = 0; i < n; i++) {
            String parameterName = openMethod.getSignature().getParameterName(i);
            Object variable = execution.getVariable(parameterName);
            if (variable != null) {
                IOpenClass parameterClass = openMethod.getSignature().getParameterType(i);
                IOpenCast openCast = convertor.getConvertor(parameterClass.getInstanceClass(), variable.getClass());
                openCasts[i] = openCast;
                if (openCast == null) {
                    f = false;
                    break;
                }
            }
        }
        if (f) {
            Object[] args = new Object[n];
            for (int i = 0; i < n; i++) {
                String parameterName = openMethod.getSignature().getParameterName(i);
                Object variable = execution.getVariable(parameterName);
                if (openCasts[i] != null) {
                    args[i] = openCasts[i].convert(variable);
                } else {
                    args[i] = variable;
                }
            }
            SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            if (isProvideRuntimeContext(execution)) {
                env.setContext(buildRuntimeContext(execution));
            }
            if (compiledInstance == null) {
                compiledInstance = compiledOpenClass.getOpenClass().newInstance(new SimpleRulesVM().getRuntimeEnv());
            }

            return openMethod.invoke(compiledInstance, args, env);
        } else {
            StringBuilder parameterNames = new StringBuilder();
            for (int i = 0; i < n; i++) {
                String parameterName = openMethod.getSignature().getParameterName(i);
                if (i != 0) {
                    parameterNames.append(", ");
                }
                parameterNames.append(parameterName);
            }
            throw new MethodNotFoundException(
                String.format("Variables for method '%s' is not found. Variable names: %s",
                    methodValue,
                    parameterNames.toString()));
        }
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Object result = invokeOpenMethod(execution);
        String resultValue = (String) resultVariable.getValue(execution);
        execution.setVariable(resultValue, result);
    }

}
