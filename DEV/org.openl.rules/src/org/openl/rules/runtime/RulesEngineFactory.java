package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.classloader.OpenLClassLoader;
import org.openl.conf.UserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.ASMProxyFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IOpenLMethodHandler;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.util.ClassUtils;
import org.openl.validation.ValidatedCompiledOpenClass;
import org.openl.validation.ValidationManager;
import org.openl.vm.IRuntimeEnv;
import org.openl.xls.RulesCompileContext;

/**
 * Simple engine factory Requiring only source of rules and generates interface for it if service interface not defined.
 *
 * @author PUdalau, Marat Kamalov
 */
public class RulesEngineFactory<T> {


    public static final String DEFAULT_USER_HOME = ".";
    private static final String INCORRECT_RET_TYPE_MSG = "Expected return type '%s' for method '%s', but found '%s'.";
    private final Logger log = LoggerFactory.getLogger(RulesEngineFactory.class);
    private final IOpenSourceCodeModule sourceCode;
    private final String userHome;

    private InterfaceClassGenerator interfaceClassGenerator = new InterfaceClassGenerator();
    private Class<T> interfaceClass;
    private CompiledOpenClass compiledOpenClass;
    private boolean executionMode;
    private IDependencyManager dependencyManager;
    private final IRuntimeEnvBuilder runtimeEnvBuilder = () -> new SimpleRulesVM().getRuntimeEnv();
    // Volatile is required for correct double locking checking pattern
    private volatile OpenL openl;

    public void setInterfaceClassGenerator(InterfaceClassGenerator interfaceClassGenerator) {
        this.interfaceClassGenerator = Objects.requireNonNull(interfaceClassGenerator,
                "interfaceClassGenerator cannot be null");
    }

    public RulesEngineFactory(String sourceFile) {
        userHome = DEFAULT_USER_HOME;
        sourceCode = new URLSourceCodeModule(sourceFile);
    }

    public RulesEngineFactory(String sourceFile, Class<T> interfaceClass) {
        userHome = DEFAULT_USER_HOME;
        this.sourceCode = new URLSourceCodeModule(sourceFile);
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String sourceFile, String userHome) {
        this.userHome = userHome;
        sourceCode = new URLSourceCodeModule(sourceFile);
    }

    public RulesEngineFactory(String sourceFile, String userHome, Class<T> interfaceClass) {
        this.userHome = userHome;
        setInterfaceClass(interfaceClass);
        sourceCode = new URLSourceCodeModule(sourceFile);
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule) {
        userHome = DEFAULT_USER_HOME;
        this.sourceCode = sourceCodeModule;
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule, Class<T> interfaceClass) {
        userHome = DEFAULT_USER_HOME;
        this.sourceCode = sourceCodeModule;
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome) {
        this.userHome = userHome;
        this.sourceCode = source;
    }

    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome, Class<T> interfaceClass) {
        this.userHome = userHome;
        this.sourceCode = source;
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(URL source) {
        userHome = DEFAULT_USER_HOME;
        this.sourceCode = new URLSourceCodeModule(source);
    }

    public RulesEngineFactory(URL source, Class<T> interfaceClass) {
        userHome = DEFAULT_USER_HOME;
        this.sourceCode = new URLSourceCodeModule(source);
        Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
        setInterfaceClass(interfaceClass);
    }

    public void reset() {
        compiledOpenClass = null;
    }

    /**
     * Creates java interface for rules project.
     *
     * @return interface for rules project.
     */
    public Class<T> getInterfaceClass() {
        if (interfaceClass == null) {
            IOpenClass openClass = getCompiledOpenClass().getOpenClassWithErrors();
            String className = openClass.getName();
            ClassLoader classLoader = getCompiledOpenClass().getClassLoader();
            try {
                try {
                    @SuppressWarnings("unchecked")
                    Class<T> interfaceClass = (Class<T>) classLoader.loadClass(className);
                    log.warn("Previously generated interface '{}' has been used as a service class.", className);
                    setInterfaceClass(interfaceClass);
                    return interfaceClass;
                } catch (ClassNotFoundException e) {
                    @SuppressWarnings("unchecked")
                    Class<T> interfaceClass = (Class<T>) interfaceClassGenerator
                            .generateInterface(className, openClass, classLoader);
                    setInterfaceClass(interfaceClass);
                    return interfaceClass;
                }
            } catch (Exception e) {
                throw new OpenlNotCheckedException("Failed to generate the interface '" + className + "'", e);
            }
        } else {
            return interfaceClass;
        }
    }

    private IOpenLMethodHandler prepareMethodHandler(Object openClassInstance,
                                                       Map<Method, IOpenMember> methodMap,
                                                       IRuntimeEnv runtimeEnv) {
        OpenLRulesMethodHandler openLRulesMethodHandler = new OpenLRulesMethodHandler(openClassInstance,
                methodMap,
                runtimeEnvBuilder);
        if (runtimeEnv != null) {
            openLRulesMethodHandler.setRuntimeEnv(runtimeEnv);
        }
        return openLRulesMethodHandler;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    private CompiledOpenClass initializeOpenClass() {
        boolean oldValidationState = ValidationManager.isValidationEnabled();
        CompiledOpenClass compiledOpenClass;
        try {
            ValidationManager.turnOffValidation();
            CompiledOpenClass result;
            // Change class loader to OpenLBundleClassLoader
            //
            //
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                // if current bundle is dependency of parent bundle it must be
                // visible
                // for parent bundle
                //
                if (!(oldClassLoader instanceof OpenLClassLoader)) {
                    ClassLoader newClassLoader = new OpenLClassLoader(oldClassLoader);
                    Thread.currentThread().setContextClassLoader(newClassLoader);
                }

                result = OpenLManager.compileModuleWithErrors(getOpenL(), sourceCode, executionMode, dependencyManager);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            compiledOpenClass = result;
        } finally {
            if (oldValidationState) {
                ValidationManager.turnOnValidation();
            }
        }
        try {
            if (oldValidationState) {
                IBindingContext bindingContext = getOpenL().getBinder().makeBindingContext();
                bindingContext.setExecutionMode(isExecutionMode());
                ValidationManager
                        .validate(new RulesCompileContext(), compiledOpenClass.getOpenClassWithErrors(), bindingContext);
                ValidatedCompiledOpenClass validatedCompiledOpenClass = ValidatedCompiledOpenClass
                        .instanceOf(compiledOpenClass);
                if (bindingContext.getMessages() != null) {
                    bindingContext.getMessages().forEach(validatedCompiledOpenClass::addMessage);
                }
                return validatedCompiledOpenClass;
            }
            return compiledOpenClass;
        } finally {
            // Turning off validation disables cleaning up, because validation works with tsn nodes
            if (isExecutionMode()) {
                ((XlsModuleOpenClass) compiledOpenClass.getOpenClassWithErrors()).clearForExecutionMode();
            }
        }
    }

    private Object prepareInstance(IRuntimeEnv runtimeEnv, boolean ignoreCompilationErrors) {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = ignoreCompilationErrors ? compiledOpenClass.getOpenClassWithErrors()
                    : compiledOpenClass.getOpenClass();
            Class<T> interfaceClass = getInterfaceClass();
            Map<Method, IOpenMember> methodMap = prepareMethodMap(interfaceClass, openClass);
            Object openClassInstance = openClass
                    .newInstance(runtimeEnv == null ? runtimeEnvBuilder.buildRuntimeEnv() : runtimeEnv);
            ClassLoader classLoader = interfaceClass.getClassLoader();

            Class<?>[] proxyInterfaces = new Class[]{interfaceClass, IEngineWrapper.class, IRulesRuntimeContextProvider.class};

            return ASMProxyFactory.newProxyInstance(classLoader,
                    prepareMethodHandler(openClassInstance, methodMap, runtimeEnv),
                    proxyInterfaces);
        } catch (OpenlNotCheckedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenlNotCheckedException("Failed to instantiate engine instance.", ex);
        }
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }

    public boolean isExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(boolean executionMode) {
        this.executionMode = executionMode;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    private OpenL getOpenL() {
        if (openl == null) {
            synchronized (this) {
                if (openl == null) {
                    var userContext = new UserContext(ClassUtils.getCurrentClassLoader(getClass()), userHome);
                    openl = OpenL.getInstance(OpenL.OPENL_JAVA_RULE_NAME, userContext);
                }
            }
        }
        return openl;
    }

    public T newEngineInstance() {
        return newEngineInstance(false);
    }

    @SuppressWarnings("unchecked")
    public T newEngineInstance(boolean ignoreCompilationErrors) {
        return (T) prepareInstance(null, ignoreCompilationErrors);
    }

    /**
     * Creates methods map that contains interface's methods as key and appropriate open class's members as value.
     *
     * @param engineInterface interface that provides method for engine
     * @param moduleOpenClass open class that used by engine to invoke appropriate rules
     * @return methods map
     */
    private Map<Method, IOpenMember> prepareMethodMap(Class<?> engineInterface, IOpenClass moduleOpenClass) {

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

    private void validateReturnType(IOpenMethod openMethod, Method interfaceMethod) {
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
}
