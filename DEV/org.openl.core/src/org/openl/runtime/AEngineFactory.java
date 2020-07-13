package org.openl.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public abstract class AEngineFactory {

    private static final String INCORRECT_RET_TYPE_MSG = "Expected return type '%s' for method '%s', but found '%s'.";

    public final Object newInstance() {
        return newInstance(getRuntimeEnvBuilder().buildRuntimeEnv());
    }

    protected final Object prepareProxyInstance(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv,
            ClassLoader classLoader) {

        Class<?>[] proxyInterfaces = prepareInstanceInterfaces();

        return ASMProxyFactory.newProxyInstance(classLoader,
            prepareMethodHandler(openClassInstance, methodMap, runtimeEnv),
            proxyInterfaces);
    }

    public final Object newInstance(IRuntimeEnv runtimeEnv) {
        if (runtimeEnv == null) {
            return prepareInstance(getRuntimeEnvBuilder().buildRuntimeEnv());
        } else {
            return prepareInstance(runtimeEnv);
        }
    }

    protected abstract Object prepareInstance(IRuntimeEnv runtimeEnv);

    protected abstract Class<?>[] prepareInstanceInterfaces();

    protected abstract IRuntimeEnvBuilder getRuntimeEnvBuilder();

    protected abstract IOpenLMethodHandler prepareMethodHandler(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv);

    public abstract CompiledOpenClass getCompiledOpenClass();

    /**
     * Creates methods map that contains interface's methods as key and appropriate open class's members as value.
     *
     * @param engineInterface interface that provides method for engine
     * @param moduleOpenClass open class that used by engine to invoke appropriate rules
     * @return methods map
     */
    protected Map<Method, IOpenMember> prepareMethodMap(Class<?> engineInterface, IOpenClass moduleOpenClass) {

        // Methods map.
        //
        Map<Method, IOpenMember> methodMap = new HashMap<>();
        // Get declared by engine interface methods.
        //
        Method[] interfaceMethods = engineInterface.getDeclaredMethods();

        for (Method interfaceMethod : interfaceMethods) {
            // Get name of method.
            //
            String interfaceMethodName = interfaceMethod.getName();
            // Try to find openClass's method with appropriate name and
            // parameter types.
            //
            IOpenMethod rulesMethod = OpenClassHelper.findRulesMethod(moduleOpenClass, interfaceMethod);

            if (rulesMethod != null) {
                validateReturnType(rulesMethod, interfaceMethod);
                // If openClass has appropriate method then add new entry to
                // methods map.
                //
                methodMap.put(interfaceMethod, rulesMethod);
            } else {
                // Try to find appropriate method candidate in openClass's
                // fields.
                //
                IOpenField ruleField = OpenClassHelper.findRulesField(moduleOpenClass, interfaceMethod);
                if (ruleField != null) {
                    methodMap.put(interfaceMethod, ruleField);
                    continue;
                } else {
                    if (interfaceMethod.getParameterCount() == 0) {
                        IOpenField openField = OpenClassHelper.findRulesField(moduleOpenClass,
                            interfaceMethod.getName());
                        if (openField != null) {
                            String message = String.format(INCORRECT_RET_TYPE_MSG,
                                openField.getType(),
                                interfaceMethodName,
                                interfaceMethod.getName());
                            throw new RuntimeException(message);
                        }
                    }
                }
                // If openClass does not have appropriate method or field then
                // throw runtime exception.
                //
                String message = String.format("There is no implementation in rules for interface method '%s'",
                    interfaceMethod);

                throw new OpenlNotCheckedException(message);
            }
        }

        return methodMap;
    }

    protected void validateReturnType(IOpenMethod openMethod, Method interfaceMethod) {
        Class<?> interfaceReturnType = interfaceMethod.getReturnType();
        Class<?> openClassReturnType = openMethod.getType().getInstanceClass();
        boolean isAssignable = ClassUtils.isAssignable(openClassReturnType, interfaceReturnType);
        if (!isAssignable) {
            String message = String.format(INCORRECT_RET_TYPE_MSG,
                openClassReturnType.getName(),
                interfaceMethod.getName(),
                interfaceReturnType.getName());
            throw new ClassCastException(message);
        }
    }

}
