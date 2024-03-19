package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.net.URL;
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
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.AEngineFactory;
import org.openl.runtime.ASMProxyFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IOpenLMethodHandler;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.validation.ValidatedCompiledOpenClass;
import org.openl.validation.ValidationManager;
import org.openl.vm.IRuntimeEnv;
import org.openl.xls.RulesCompileContext;

/**
 * Simple engine factory Requiring only source of rules and generates interface for it if service interface not defined.
 *
 * @author PUdalau, Marat Kamalov
 */
public class RulesEngineFactory<T> extends AEngineFactory {

    private final Logger log = LoggerFactory.getLogger(RulesEngineFactory.class);
    private final IOpenSourceCodeModule sourceCode;

    private InterfaceClassGenerator interfaceClassGenerator = new InterfaceClassGeneratorImpl();
    private Class<T> interfaceClass;
    private CompiledOpenClass compiledOpenClass;
    private boolean executionMode;
    private IDependencyManager dependencyManager;

    public void setInterfaceClassGenerator(InterfaceClassGenerator interfaceClassGenerator) {
        this.interfaceClassGenerator = Objects.requireNonNull(interfaceClassGenerator,
                "interfaceClassGenerator cannot be null");
    }

    public RulesEngineFactory(String sourceFile) {
        super(OpenL.OPENL_JAVA_RULE_NAME);
        sourceCode = new URLSourceCodeModule(sourceFile);
    }

    public RulesEngineFactory(String sourceFile, Class<T> interfaceClass) {
        super(OpenL.OPENL_JAVA_RULE_NAME);
        this.sourceCode = new URLSourceCodeModule(sourceFile);
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String sourceFile, String userHome) {
        super(OpenL.OPENL_JAVA_RULE_NAME, userHome);
        sourceCode = new URLSourceCodeModule(sourceFile);
    }

    public RulesEngineFactory(String sourceFile, String userHome, Class<T> interfaceClass) {
        super(OpenL.OPENL_JAVA_RULE_NAME, userHome);
        setInterfaceClass(interfaceClass);
        sourceCode = new URLSourceCodeModule(sourceFile);
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule) {
        super(OpenL.OPENL_JAVA_RULE_NAME);
        this.sourceCode = sourceCodeModule;
    }

    public RulesEngineFactory(IOpenSourceCodeModule sourceCodeModule, Class<T> interfaceClass) {
        super(OpenL.OPENL_JAVA_RULE_NAME);
        this.sourceCode = sourceCodeModule;
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome) {
        super(OpenL.OPENL_JAVA_RULE_NAME, userHome);
        this.sourceCode = source;
    }

    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome, Class<T> interfaceClass) {
        super(OpenL.OPENL_JAVA_RULE_NAME, userHome);
        this.sourceCode = source;
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(URL source) {
        super(OpenL.OPENL_JAVA_RULE_NAME);
        this.sourceCode = new URLSourceCodeModule(source);
    }

    public RulesEngineFactory(URL source, Class<T> interfaceClass) {
        super(OpenL.OPENL_JAVA_RULE_NAME);
        this.sourceCode = new URLSourceCodeModule(source);
        Objects.requireNonNull(interfaceClass, "interfaceClass cannot be null");
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, Class<T> interfaceClass) {
        super(openlName);
        this.sourceCode = sourceCode;
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String openlName, IOpenSourceCodeModule sourceCode) {
        super(openlName);
        this.sourceCode = sourceCode;
    }

    public RulesEngineFactory(String openlName,
                              String userHome,
                              IOpenSourceCodeModule sourceCode,
                              Class<T> interfaceClass) {
        super(openlName, userHome);
        this.sourceCode = sourceCode;
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String openlName, String userHome, IOpenSourceCodeModule sourceCode) {
        super(openlName, userHome);
        this.sourceCode = sourceCode;
    }

    public RulesEngineFactory(String openlName, String userHome, String sourceFile, Class<T> interfaceClass) {
        super(openlName, userHome);
        sourceCode = new URLSourceCodeModule(sourceFile);
        setInterfaceClass(interfaceClass);
    }

    public RulesEngineFactory(String openlName, String userHome, String sourceFile) {
        super(openlName, userHome);
        this.sourceCode = new URLSourceCodeModule(sourceFile);
    }

    /**
     * Added to allow using other openl names, such as org.openl.xls.ce
     */
    public RulesEngineFactory(IOpenSourceCodeModule source, String userHome, String openlName) {
        super(openlName, userHome);
        this.sourceCode = source;
    }

    public void reset(boolean resetInterface) {
        reset();
        if (resetInterface) {
            setInterfaceClass(null);
        }
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

    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class[]{IEngineWrapper.class, IRulesRuntimeContextProvider.class};
    }

    private IRuntimeEnvBuilder runtimeEnvBuilder = null;

    @Override
    protected IRuntimeEnvBuilder getRuntimeEnvBuilder() {
        if (runtimeEnvBuilder == null) {
            runtimeEnvBuilder = () -> new SimpleRulesVM().getRuntimeEnv();
        }
        return runtimeEnvBuilder;
    }

    @Override
    protected IOpenLMethodHandler prepareMethodHandler(Object openClassInstance,
                                                       Map<Method, IOpenMember> methodMap,
                                                       IRuntimeEnv runtimeEnv) {
        OpenLRulesMethodHandler openLRulesMethodHandler = new OpenLRulesMethodHandler(openClassInstance,
                methodMap,
                getRuntimeEnvBuilder());
        if (runtimeEnv != null) {
            openLRulesMethodHandler.setRuntimeEnv(runtimeEnv);
        }
        return openLRulesMethodHandler;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    protected CompiledOpenClass initializeOpenClass() {
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

                result = OpenLManager.compileModuleWithErrors(getOpenL(), getSourceCode(), executionMode, dependencyManager);
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

    public T newEngineInstance() {
        return newEngineInstance(false);
    }

    public T newEngineInstance(IRuntimeEnv runtimeEnv) {
        return newEngineInstance(runtimeEnv, false);
    }

    @SuppressWarnings("unchecked")
    public T newEngineInstance(IRuntimeEnv runtimeEnv, boolean ignoreCompilationErrors) {
        return (T) newInstance(runtimeEnv, ignoreCompilationErrors);
    }

    @SuppressWarnings("unchecked")
    public T newEngineInstance(boolean ignoreCompilationErrors) {
        return (T) newInstance(ignoreCompilationErrors);
    }

    public void reset() {
        compiledOpenClass = null;
    }

    @Override
    public Object prepareInstance(IRuntimeEnv runtimeEnv, boolean ignoreCompilationErrors) {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = ignoreCompilationErrors ? compiledOpenClass.getOpenClassWithErrors()
                    : compiledOpenClass.getOpenClass();
            Class<T> interfaceClass = getInterfaceClass();
            Map<Method, IOpenMember> methodMap = prepareMethodMap(interfaceClass, openClass);
            Object openClassInstance = openClass
                    .newInstance(runtimeEnv == null ? getRuntimeEnvBuilder().buildRuntimeEnv() : runtimeEnv);
            ClassLoader classLoader = interfaceClass.getClassLoader();

            Class<?>[] interfaces = prepareInstanceInterfaces();

            var proxyInterfaces = new Class[interfaces.length + 1];
            proxyInterfaces[0] = interfaceClass;
            System.arraycopy(interfaces, 0, proxyInterfaces, 1, interfaces.length);

            return ASMProxyFactory.newProxyInstance(classLoader,
                    prepareMethodHandler(openClassInstance, methodMap, runtimeEnv),
                    proxyInterfaces);
        } catch (OpenlNotCheckedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenlNotCheckedException("Failed to instantiate engine instance.", ex);
        }
    }

    @Override
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

    public IOpenSourceCodeModule getSourceCode() {
        return sourceCode;
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }
}
