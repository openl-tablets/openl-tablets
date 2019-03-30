package org.openl.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    private static final String INCORRECT_RET_TYPE_MSG = "Return type of method \"%s\" should be %s";

    /**
     * This method deprecated. Use newInstance method.
     */
    @Deprecated
    public final Object makeInstance() {
        return newInstance();
    }

    public final Object newInstance() {
        return newInstance(getRuntimeEnvBuilder().buildRuntimeEnv());
    }

    protected final Object prepareProxyInstance(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv,
            ClassLoader classLoader) {

        Class<?>[] proxyInterfaces = prepareInstanceInterfaces();

        InvocationHandler handler = prepareInvocationHandler(openClassInstance, methodMap, runtimeEnv);

        return Proxy.newProxyInstance(classLoader, proxyInterfaces, handler);
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

    protected abstract InvocationHandler prepareInvocationHandler(Object openClassInstance,
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
            IOpenClass[] params = OpenClassHelper.getOpenClasses(moduleOpenClass, interfaceMethod.getParameterTypes());
            IOpenMethod rulesMethod = moduleOpenClass.getMethod(interfaceMethodName, params);

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
                if (interfaceMethodName.startsWith("get")) {
                    // Build field name to find.
                    //
                    String fieldName = ClassUtils.toFieldName(interfaceMethodName);
                    // Try to find appropriate field.
                    //
                    IOpenField rulesField = moduleOpenClass.getField(fieldName, true);

                    if (rulesField == null) {
                        fieldName = ClassUtils.capitalize(fieldName);
                        rulesField = moduleOpenClass.getField(fieldName, true);
                    }

                    if (rulesField != null) {
                        // Cast method return type to appropriate OpenClass
                        // type.
                        //
                        IOpenClass methodReturnType = OpenClassHelper.getOpenClass(moduleOpenClass,
                            interfaceMethod.getReturnType());

                        if (methodReturnType.getInstanceClass()
                            .isAssignableFrom(rulesField.getType().getInstanceClass())) {
                            // If openClass's field type is equal to method
                            // return
                            // type then add new entry to methods map.
                            //
                            methodMap.put(interfaceMethod, rulesField);
                            // Jump to the next interface method.
                            //
                            continue;
                        } else {
                            // If openClass doesn't have appropriate field
                            // (field's type doesn't
                            // equal to method return type) then throw runtime
                            // exception.
                            //
                            String message = String
                                .format(INCORRECT_RET_TYPE_MSG, interfaceMethodName, rulesField.getType());

                            throw new RuntimeException(message);
                        }
                    }
                }

                // If openClass doesn't have appropriate method or field then
                // throw runtime exception.
                //
                String message = String.format("There is no implementation in rules for interface method \"%s\"",
                    interfaceMethod);

                throw new OpenlNotCheckedException(message);
            }
        }

        return methodMap;
    }

    protected void validateReturnType(IOpenMethod openMethod, Method interfaceMethod) {
        Class<?> returnType = interfaceMethod.getReturnType();
        Class<?> openClassReturnType = openMethod.getType().getInstanceClass();
        boolean isAssignable = ClassUtils.isAssignable(openClassReturnType, returnType);
        if (!isAssignable) {
            String message = String
                .format(INCORRECT_RET_TYPE_MSG, interfaceMethod.getName(), openClassReturnType.getName());
            throw new RuntimeException(message);
        }
    }

}
