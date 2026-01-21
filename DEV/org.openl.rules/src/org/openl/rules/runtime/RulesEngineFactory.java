package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.ASMProxyFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.validation.ValidatedCompiledOpenClass;
import org.openl.validation.ValidationManager;
import org.openl.xls.Parser;
import org.openl.xls.RulesCompileContext;

/**
 * Simple engine factory Requiring only source of rules and generates interface for it if service interface not defined.
 *
 * @author PUdalau, Marat Kamalov
 */
public class RulesEngineFactory<T> {


    private static final String INCORRECT_RET_TYPE_MSG = "Expected return type '%s' for method '%s', but found '%s'.";
    private final Logger log = LoggerFactory.getLogger(RulesEngineFactory.class);
    private final IOpenSourceCodeModule sourceCode;

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
        sourceCode = new URLSourceCodeModule(sourceFile);
    }

    public RulesEngineFactory(String sourceFile, Class<T> interfaceClass) {
        this.sourceCode = new URLSourceCodeModule(sourceFile);
        this.interfaceClass = interfaceClass;
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule) {
        this.sourceCode = sourceCodeModule;
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule, Class<T> interfaceClass) {
        this.sourceCode = sourceCodeModule;
        this.interfaceClass = interfaceClass;
    }

    public RulesEngineFactory(URL source) {
        this.sourceCode = new URLSourceCodeModule(source);
    }

    public RulesEngineFactory(URL source, Class<T> interfaceClass) {
        this.sourceCode = new URLSourceCodeModule(source);
        this.interfaceClass = interfaceClass;
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
                    interfaceClass = (Class<T>) classLoader.loadClass(className);
                    log.warn("Previously generated interface '{}' has been used as a service class.", className);
                } catch (ClassNotFoundException e) {
                    interfaceClass = (Class<T>) interfaceClassGenerator.generateInterface(className, openClass, classLoader);
                }
            } catch (Exception | LinkageError e) {
                throw new OpenlNotCheckedException("Failed to generate the interface '" + className + "'", e);
            }
        }
        return interfaceClass;
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
                    openl = new OpenL();
                    openl.setParser(new Parser());
                    openl.setBinder(new XlsBinder(new RulesCompileContext()));
                    openl.setVm(new SimpleRulesVM());
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
        try {
            compiledOpenClass = getCompiledOpenClass();
            var openClass = ignoreCompilationErrors ? compiledOpenClass.getOpenClassWithErrors()
                    : compiledOpenClass.getOpenClass();
            var clz = getInterfaceClass();
            var methodMap = prepareMethodMap(clz, openClass);
            var openClassInstance = openClass.newInstance(runtimeEnvBuilder.buildRuntimeEnv());
            var proxyInterfaces = new Class[]{clz, IEngineWrapper.class};
            var handler = new OpenLRulesMethodHandler(openClassInstance, methodMap, runtimeEnvBuilder);
            return (T) ASMProxyFactory.newProxyInstance(clz.getClassLoader(), handler, proxyInterfaces);
        } catch (OpenlNotCheckedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenlNotCheckedException("Failed to instantiate engine instance.", ex);
        }
    }

    /**
     * Creates methods map that contains interface's methods as key and appropriate open class's members as value.
     *
     * @param engineInterface interface that provides method for engine
     * @param openClass open class that used by engine to invoke appropriate rules
     * @return methods map
     */
    private static Map<Method, IOpenMember> prepareMethodMap(Class<?> engineInterface, IOpenClass openClass) {

        var methodMap = new HashMap<Method, IOpenMember>();

        for (Method interfaceMethod : engineInterface.getDeclaredMethods()) {
            var methodName = interfaceMethod.getName();
            var parameterTypes = interfaceMethod.getParameterTypes();
            if (parameterTypes.length > 0 && parameterTypes[0] == IRulesRuntimeContext.class) {
                // Skip first parameter if it is of IRulesRuntimeContext type.
                //
                parameterTypes = Arrays.copyOfRange(parameterTypes, 1, parameterTypes.length);
            }

            // Try to find openClass's method with appropriate name and
            // parameter types.
            //
            var args = new IOpenClass[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                args[i] = JavaOpenClass.getOpenClass(parameterTypes[i]);
            }
            IOpenMember rulesMethod = openClass.getMethod(methodName, args);

            if (rulesMethod == null && methodName.startsWith("get") && parameterTypes.length == 0) {
                // Try to find appropriate method candidate in openClass's
                // fields.
                //
                var fieldName = methodName.substring(3);
                rulesMethod = openClass.getField(fieldName);
                if (rulesMethod == null) {
                    rulesMethod = openClass.getField(ClassUtils.decapitalize(fieldName));
                }
            }

            if (rulesMethod == null) {
                // If openClass does not have appropriate method or field then
                // throw runtime exception.
                //
                String message = String.format("There is no implementation in rules for interface method '%s'",
                        interfaceMethod);

                throw new OpenlNotCheckedException(message);
            }

            validateReturnType(rulesMethod, interfaceMethod);
            // If openClass has appropriate method then add new entry to
            // methods map.
            //
            methodMap.put(interfaceMethod, rulesMethod);
        }

        return methodMap;
    }

    private static void validateReturnType(IOpenMember openMethod, Method interfaceMethod) {
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
