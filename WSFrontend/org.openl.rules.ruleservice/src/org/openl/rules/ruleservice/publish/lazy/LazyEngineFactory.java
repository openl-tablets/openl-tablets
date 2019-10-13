package org.openl.rules.ruleservice.publish.lazy;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLSourceManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.runtime.AOpenLRulesEngineFactory;
import org.openl.rules.runtime.InterfaceClassGenerator;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prebinds openclass and creates LazyMethod and LazyField that will compile neccessary modules on demand.
 *
 * @author PUdalau, Marat Kamalov
 */
public class LazyEngineFactory<T> extends AOpenLRulesEngineFactory {

    private static final String RULES_XLS_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    static {
        OpenL.setConfig(new LazyOpenLConfigurator());
    }

    private final Logger log = LoggerFactory.getLogger(LazyEngineFactory.class);

    private CompiledOpenClass compiledOpenClass;
    private Class<T> interfaceClass;
    private Collection<Module> modules;
    private IDependencyManager dependencyManager;
    private Map<String, Object> externalParameters;
    private DeploymentDescription deployment;

    public DeploymentDescription getDeployment() {
        return deployment;
    }

    private InterfaceClassGenerator interfaceClassGenerator = new InterfaceClassGeneratorImpl();

    public void setInterfaceClassGenerator(InterfaceClassGenerator interfaceClassGenerator) {
        this.interfaceClassGenerator = Objects.requireNonNull(interfaceClassGenerator,
            "interfaceClassGenerator can't be null.");
        if (interfaceClass != null) {
            log.warn("Rules engine factory already has interface class. Interface class generator has been ignored.");
        }
    }

    public InterfaceClassGenerator getInterfaceClassGenerator() {
        return interfaceClassGenerator;
    }

    /**
     * Added to allow using openl that is different from default, such as org.openl.xls.ce
     *
     * @param modules
     * @param openlName `
     */
    public LazyEngineFactory(DeploymentDescription deployment, Collection<Module> modules) {
        super(RULES_XLS_OPENL_NAME);
        this.deployment = Objects.requireNonNull(deployment, "deployment can't be null.");
        this.modules = modules;
    }

    public LazyEngineFactory(DeploymentDescription deployment,
            Collection<Module> modules,
            IDependencyManager dependencyManager) {
        this(deployment, modules);
        this.dependencyManager = dependencyManager;
    }

    public LazyEngineFactory(DeploymentDescription deployment,
            Collection<Module> modules,
            IDependencyManager dependencyManager,
            Map<String, Object> externalParameters) {
        this(deployment, modules, dependencyManager);
        this.externalParameters = externalParameters;
    }

    public LazyEngineFactory(DeploymentDescription deployment, Collection<Module> modules, Class<T> interfaceClass) {
        this(deployment, modules);
        this.interfaceClass = interfaceClass;
    }

    public LazyEngineFactory(DeploymentDescription deployment,
            Collection<Module> modules,
            Class<T> interfaceClass,
            Map<String, Object> externalParameters) {
        this(deployment, modules, interfaceClass);
        this.externalParameters = externalParameters;
    }

    public LazyEngineFactory(DeploymentDescription deployment,
            Collection<Module> modules,
            IDependencyManager dependencyManager,
            Class<T> interfaceClass) {
        this(deployment, modules, dependencyManager);
        this.interfaceClass = interfaceClass;
    }

    public LazyEngineFactory(DeploymentDescription deployment,
            Collection<Module> modules,
            IDependencyManager dependencyManager,
            Class<T> interfaceClass,
            Map<String, Object> externalParameters) {
        this(deployment, modules, dependencyManager, interfaceClass);
        this.externalParameters = externalParameters;
    }

    @Override
    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }

    @SuppressWarnings({ "unchecked" })
    public Class<T> getInterfaceClass() {
        if (interfaceClass == null) {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            final String className = openClass.getName();
            try {
                interfaceClass = (Class<T>) interfaceClassGenerator
                    .generateInterface(className, openClass, getCompiledOpenClass().getClassLoader());
            } catch (Exception e) {
                String errorMessage = String.format("Failed to create interface: %s", className);
                log.error(errorMessage, e);
                throw new OpenlNotCheckedException(errorMessage, e);
            }
        }
        return interfaceClass;
    }

    @Override
    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class[] { getInterfaceClass(), IEngineWrapper.class, IRulesRuntimeContextProvider.class };
    }

    @Override
    protected Object prepareInstance(IRuntimeEnv runtimeEnv) {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            Object openClassInstance = openClass.newInstance(runtimeEnv);
            Map<Method, IOpenMember> methodMap = prepareMethodMap(getInterfaceClass(), openClass);

            return prepareProxyInstance(openClassInstance,
                methodMap,
                runtimeEnv,
                getCompiledOpenClass().getClassLoader());
        } catch (Exception ex) {
            String errorMessage = "Failed to instantiate engine instance!";
            throw new OpenlNotCheckedException(errorMessage, ex);
        }
    }

    Module getModuleForMember(IOpenMember member) {
        String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
        Module module = getModuleForSourceUrl(sourceUrl, modules);
        if (module != null) {
            return module;
        }
        throw new OpenlNotCheckedException("Module is not found");
    }

    private Module getModuleForSourceUrl(String sourceUrl, Collection<Module> modules) {
        if (modules.size() == 1) {
            return modules.iterator().next();
        }
        for (Module module : modules) {
            String modulePath = module.getRulesRootPath().getPath();
            try {
                if (FilenameUtils.normalize(sourceUrl)
                    .equals(FilenameUtils
                        .normalize(new File(modulePath).getCanonicalFile().toURI().toURL().toExternalForm()))) {
                    return module;
                }
            } catch (Exception e) {
                log.warn("Failed to build url for module '{}' with path: {}", module.getName(), modulePath, e);
            }
        }
        return null;
    }

    private LazyMethod makeLazyMethod(XlsLazyModuleOpenClass xlsLazyModuleOpenClass, IOpenMethod method) {
        final Module declaringModule = getModuleForMember(method);
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }

        return LazyMethod.getLazyMethod(xlsLazyModuleOpenClass,
            deployment,
            declaringModule,
            argTypes,
            method,
            dependencyManager,
            Thread.currentThread().getContextClassLoader(),
            true,
            externalParameters);
    }

    private LazyField makeLazyField(XlsLazyModuleOpenClass xlsLazyModuleOpenClass, IOpenField field) {
        Module declaringModule = getModuleForMember(field);
        return LazyField.getLazyField(xlsLazyModuleOpenClass,
            deployment,
            declaringModule,
            field,
            dependencyManager,
            Thread.currentThread().getContextClassLoader(),
            true,
            externalParameters);
    }

    private CompiledOpenClass initializeOpenClass() {
        // put prebinder to openl
        IPrebindHandler prebindHandler = LazyBinderInvocationHandler.getPrebindHandler();
        try {
            LazyBinderInvocationHandler.setPrebindHandler(new IPrebindHandler() {

                @Override
                public IOpenMethod processMethodAdded(IOpenMethod method, XlsLazyModuleOpenClass moduleOpenClass) {
                    return makeLazyMethod(moduleOpenClass, method);
                }

                @Override
                public IOpenField processFieldAdded(IOpenField field, XlsLazyModuleOpenClass moduleOpenClass) {
                    return makeLazyField(moduleOpenClass, field);
                }
            });

            IOpenSourceCodeModule mainModule = createMainModule();
            RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(mainModule,
                AOpenLEngineFactory.DEFAULT_USER_HOME,
                getOpenlName());// FIXME
            engineFactory.setDependencyManager(dependencyManager);
            engineFactory.setExecutionMode(true);
            CompiledOpenClass result = engineFactory.getCompiledOpenClass();
            return result;
        } finally {
            LazyBinderInvocationHandler.setPrebindHandler(prebindHandler);
        }
    }

    private IOpenSourceCodeModule createMainModule() {
        List<IDependency> dependencies = new ArrayList<>();

        for (Module module : modules) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        if (params.get(OpenLSourceManager.EXTERNAL_DEPENDENCIES_KEY) != null) {
            @SuppressWarnings("unchecked")
            List<IDependency> externalDependencies = (List<IDependency>) params
                .get(OpenLSourceManager.EXTERNAL_DEPENDENCIES_KEY);
            dependencies.addAll(externalDependencies);
        }
        params.put(OpenLSourceManager.EXTERNAL_DEPENDENCIES_KEY, dependencies);

        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }

    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    @Override
    protected void validateReturnType(IOpenMethod openMethod, Method interfaceMethod) {
        if (!(interfaceMethod.isAnnotationPresent(ServiceCallAfterInterceptor.class) || interfaceMethod
            .isAnnotationPresent(ServiceCallAroundInterceptor.class))) {
            super.validateReturnType(openMethod, interfaceMethod);
        }
    }
}
