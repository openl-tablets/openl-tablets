package org.openl.rules.ruleservice.factory;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.runtime.ASourceCodeEngineFactory;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class WebServiceRulesEngineFactory extends ASourceCodeEngineFactory {
    public static final String RULE_OPENL_NAME = "org.openl.xls";
    private static final String FIELD_PREFIX = "get";

    private IOpenClass openClass;
    private Class<?> interfaceClass;

    public WebServiceRulesEngineFactory(String sourceFile) {
        super(RULE_OPENL_NAME, sourceFile);
    }

    public WebServiceRulesEngineFactory(File file) {
        super(RULE_OPENL_NAME, file);
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }
    
    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { interfaceClass };
    }

    @Override
    public Object makeInstance() {

        try {
            openClass = initializeOpenClass();
            String className = openClass.getName();
            interfaceClass = RulesFactory.generateInterface(className, openClass, getDefaultUserClassLoader());

            IRuntimeEnv runtimeEnv = getOpenL().getVm().getRuntimeEnv();
            Object openClassInstance = openClass.newInstance(runtimeEnv);
            Map<Method, IOpenMember> methodMap = makeMethodMap(interfaceClass, openClass);

            return makeEngineInstance(openClassInstance, methodMap, runtimeEnv, getDefaultUserClassLoader());

        } catch (Exception ex) {
            throw new OpenLRuntimeException("Cannot instantiate engine instance", ex);
        }

    }

    @Override
    protected InvocationHandler makeInvocationHandler(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {

        return new WebServiceRulesInvocationHandler(openClassInstance, this, runtimeEnv, methodMap);
    }

    @Override
    protected Map<Method, IOpenMember> makeMethodMap(Class<?> interfaceClass, IOpenClass openClass) {

        Map<Method, IOpenMember> methodMap = new HashMap<Method, IOpenMember>();
        Method[] interfaceMethods = interfaceClass.getDeclaredMethods();

        for (Method interfaceMethod : interfaceMethods) {

            String interfaceMethodName = interfaceMethod.getName();
            Class<?>[] interfaceMethodParameterTypes = interfaceMethod.getParameterTypes();
            Class<?>[] parameterTypes = null;
            int paramsCount = interfaceMethodParameterTypes.length;

            if (paramsCount > 0 && IRulesRuntimeContext.class.equals(interfaceMethodParameterTypes[0])) {
                parameterTypes = (Class<?>[]) ArrayUtils.subarray(interfaceMethodParameterTypes, 1, paramsCount);
            } else {
                parameterTypes = interfaceMethodParameterTypes;
            }

            IOpenMethod rulesMethod = openClass.getMatchingMethod(interfaceMethodName,
                JavaOpenClass.getOpenClasses(parameterTypes));

            if (rulesMethod != null) {
                methodMap.put(interfaceMethod, rulesMethod);
            } else {

                if (interfaceMethodName.startsWith(FIELD_PREFIX)) {

                    String fieldName = StringUtils.uncapitalize(interfaceMethodName.substring(FIELD_PREFIX.length()));
                    IOpenField rulesField = openClass.getField(fieldName, true);

                    if (rulesField != null) {
                        if (JavaOpenClass.getOpenClass(interfaceMethod.getReturnType()).equals(rulesField.getType())) {
                            methodMap.put(interfaceMethod, rulesField);
                            continue;
                        } else {
                            String message = String.format("Return type of method \"%s\" should be %s",
                                interfaceMethodName,
                                rulesField.getType());
                            throw new RuntimeException(message);
                        }
                    }
                }

                String message = String.format("There is no implementation in rules for interface method \"%s\"",
                    interfaceMethod);
                throw new RuntimeException(message);
            }
        }

        return methodMap;
    }
}
