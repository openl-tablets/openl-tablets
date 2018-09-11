package org.openl.rules.activiti.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.repository.ProcessDefinition;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.activiti.MethodNotFoundException;
import org.openl.rules.activiti.spring.result.ResultValue;
import org.openl.rules.activiti.util.IRulesRuntimeContextUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.types.java.JavaOpenClass;

public class OpenLEngine {

    public final static Object findAndInvokeMethod(String methodName,
            Object target,
            Class<?> interfaceClass,
            Object... args) throws IllegalAccessException, InvocationTargetException {
        Method[] methods = interfaceClass.getMethods();

        Method bestMethod = null;
        IOpenCast[] bestOpenCasts = null;
        int bestDistance = -1;

        ObjectToDataOpenCastConvertor convertor = new ObjectToDataOpenCastConvertor();

        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                if (args.length == method.getParameterTypes().length) {
                    boolean f = true;
                    IOpenCast[] openCasts = new IOpenCast[args.length];
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            IOpenCast openCast = convertor
                                .getConvertor(method.getParameterTypes()[i], args[i].getClass());
                            if (openCast == null) {
                                f = false;
                                break;
                            }
                            openCasts[i] = openCast;
                        }
                    }
                    if (f) {
                        if (bestMethod == null) {
                            bestMethod = method;
                            bestOpenCasts = openCasts;
                        } else {
                            if (bestDistance < 0) {
                                int maxdiff = 0;
                                int ndiff = 0;
                                int i = 0;
                                for (IOpenCast cast : bestOpenCasts) {
                                    if (cast == null || args[i].getClass() == bestMethod.getParameterTypes()[i]) {
                                        continue;
                                    }
                                    maxdiff = Math.max(maxdiff,
                                        cast.getDistance(JavaOpenClass.getOpenClass(args[i].getClass()),
                                            JavaOpenClass.getOpenClass(bestMethod.getParameterTypes()[i])));
                                    ndiff++;
                                    i++;
                                }
                                bestDistance = maxdiff * 100 + ndiff;
                            }

                            int maxdiff = 0;
                            int ndiff = 0;
                            int i = 0;
                            for (IOpenCast cast : openCasts) {
                                if (cast == null || args[i].getClass() == method.getParameterTypes()[i]) {
                                    continue;
                                }
                                maxdiff = Math.max(maxdiff,
                                    cast.getDistance(JavaOpenClass.getOpenClass(args[i].getClass()),
                                        JavaOpenClass.getOpenClass(method.getParameterTypes()[i])));
                                ndiff++;
                                i++;
                            }
                            int distance = maxdiff * 100 + ndiff;
                            if (bestDistance > distance) {
                                bestMethod = method;
                                bestOpenCasts = openCasts;
                                bestDistance = distance;
                            }
                        }
                    }
                }
            }
        }
        if (bestMethod == null) {
            throw new MethodNotFoundException(String.format("Method '%s' is not found!", methodName));
        }
        Object[] params = new Object[args.length];
        for (int i = 0; i < bestOpenCasts.length; i++) {
            if (bestOpenCasts[i] != null) {
                params[i] = bestOpenCasts[i].convert(args[i]);
            } else {
                params[i] = args[i];
            }
        }
        Object result = bestMethod.invoke(target, params);
        return result;
    }

    public IRulesRuntimeContext buildRuntimeContext(DelegateExecution execution) {
        return IRulesRuntimeContextUtils.buildRuntimeContext(execution);
    }
    
    public ResultValue execute(DelegateExecution execution,
            String resource,
            String methodName,
            Object... args) throws Exception {
        String processDefinitionId = execution.getProcessDefinitionId();
        RepositoryService repositoryService = execution.getEngineServices().getRepositoryService();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

        @SuppressWarnings("rawtypes")
        ProjectEngineFactory projectEngineFactory = OpenLRulesHelper.getInstance()
            .get(processDefinition.getDeploymentId(), resource);
        Object instance = OpenLRulesHelper.getInstance().getInstance(processDefinition.getDeploymentId(), resource);
        Class<?> interfaceClass = projectEngineFactory.getInterfaceClass();
        assert interfaceClass != null; // Always Non-null

        Object result = org.openl.rules.activiti.spring.OpenLEngine.findAndInvokeMethod(methodName,
            instance,
            interfaceClass,
            args);

        return new ResultValue(result);

    }
}
