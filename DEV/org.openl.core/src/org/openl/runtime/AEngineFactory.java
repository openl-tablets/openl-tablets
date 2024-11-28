package org.openl.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public abstract class AEngineFactory {

    private static final String INCORRECT_RET_TYPE_MSG = "Expected return type '%s' for method '%s', but found '%s'.";

    public static final String DEFAULT_USER_HOME = ".";
    private final String userHome;
    protected IRuntimeEnvBuilder runtimeEnvBuilder;
    // Volatile is required for correct double locking checking pattern
    private volatile OpenL openl;
    private volatile IUserContext userContext;
    private String openlName;

    public AEngineFactory(String openlName) {
        this(openlName, DEFAULT_USER_HOME, null);
    }

    public AEngineFactory(String openlName, String userHome) {
        this(openlName, userHome, null);
    }

    private AEngineFactory(String openlName, String userHome, IUserContext userContext) {
        this.openlName = openlName;
        this.userHome = userHome;
        this.userContext = userContext;
    }

    public OpenL getOpenL() {
        if (openl == null) {
            synchronized (this) {
                if (openl == null) {
                    openl = OpenL.getInstance(openlName, getUserContext());
                }
            }
        }
        return openl;
    }

    public IUserContext getUserContext() {
        if (userContext == null) {
            synchronized (this) {
                if (userContext == null) {
                    userContext = new UserContext(ClassUtils.getCurrentClassLoader(getClass()), userHome);
                }
            }
        }
        return userContext;
    }

    public final Object newInstance() {
        return newInstance(false);
    }

    public final Object newInstance(boolean ignoreCompilationErrors) {
        return prepareInstance(null, ignoreCompilationErrors);
    }

    public final Object newInstance(IRuntimeEnv runtimeEnv) {
        return newInstance(runtimeEnv, false);
    }

    public final Object newInstance(IRuntimeEnv runtimeEnv, boolean ignoreCompilationErrors) {
        return prepareInstance(runtimeEnv, ignoreCompilationErrors);
    }

    protected abstract Object prepareInstance(IRuntimeEnv runtimeEnv, boolean ignoreCompilationErrors);

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
        Class<?> openClassReturnType = openMethod.getType().getInstanceClass();
        if (openClassReturnType == Void.class || openClassReturnType == void.class) {
            return;
        }
        Class<?> interfaceReturnType = interfaceMethod.getReturnType();
        boolean isAssignable = ClassUtils.isAssignable(openClassReturnType, interfaceReturnType);
        if (!isAssignable) {
            String message = String.format(INCORRECT_RET_TYPE_MSG,
                    openClassReturnType.getName(),
                    interfaceMethod.getName(),
                    interfaceReturnType.getName());
            throw new ClassCastException(message);
        }
    }


    public String getOpenlName() {
        return openlName;
    }

    protected void setOpenlName(String openlName) {
        if (this.openl != null) {
            throw new IllegalStateException("'OpenL' instance is initialized already. Cannot change OpenL name");
        }
        this.openlName = openlName;
    }

    public String getUserHome() {
        return userHome;
    }

    protected IRuntimeEnvBuilder getRuntimeEnvBuilder() {
        if (runtimeEnvBuilder == null) {
            runtimeEnvBuilder = () -> new SimpleVM().getRuntimeEnv();
        }
        return runtimeEnvBuilder;
    }

    protected IOpenLMethodHandler prepareMethodHandler(Object openClassInstance,
                                                       Map<Method, IOpenMember> methodMap,
                                                       IRuntimeEnv runtimeEnv) {
        OpenLMethodHandler openLMethodHandler = new OpenLMethodHandler(openClassInstance,
                methodMap,
                getRuntimeEnvBuilder());
        if (runtimeEnv != null) {
            openLMethodHandler.setRuntimeEnv(runtimeEnv);
        }
        return openLMethodHandler;
    }
}
