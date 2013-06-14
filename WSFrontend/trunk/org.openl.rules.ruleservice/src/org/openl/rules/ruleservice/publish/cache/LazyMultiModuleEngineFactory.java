package org.openl.rules.ruleservice.publish.cache;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentRelatedInfo;
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

/**
 * Prebinds multimodule openclass and creates LazyMethod and LazyField that will
 * compile neccessary modules on demand.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class LazyMultiModuleEngineFactory<T> extends AOpenLRulesEngineFactory {

    private static final String RULES_XLS_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    static {
        OpenL.setConfig(new LazyOpenLConfigurator());
    }

    private final Log log = LogFactory.getLog(LazyMultiModuleEngineFactory.class);

    private CompiledOpenClass compiledOpenClass;
    private Class<T> interfaceClass;
    private Collection<Module> modules;
    private IDependencyManager dependencyManager;
    private Map<String, Object> externalParameters;

    private InterfaceClassGenerator interfaceClassGenerator = new InterfaceClassGeneratorImpl();

    public void setInterfaceClassGenerator(InterfaceClassGenerator interfaceClassGenerator) {
        if (interfaceClassGenerator == null) {
            throw new IllegalArgumentException("interfaceClassGenerator argument can't be null");
        }
        if (interfaceClass != null) {
            if (log.isWarnEnabled()) {
                log.warn("Rules engine factory has already had interface class. Interface class generator will be ignored!");
            }
        }
        this.interfaceClassGenerator = interfaceClassGenerator;
    }

    public InterfaceClassGenerator getInterfaceClassGenerator() {
        return interfaceClassGenerator;
    }

    /**
     * Added to allow using openl that is different from default, such as
     * org.openl.xls.ce
     * 
     * @param modules
     * @param openlName `
     */
    public LazyMultiModuleEngineFactory(Collection<Module> modules) {
        super(RULES_XLS_OPENL_NAME);
        this.modules = modules;
    }

    public LazyMultiModuleEngineFactory(Collection<Module> modules, IDependencyManager dependencyManager) {
        this(modules);
        this.dependencyManager = dependencyManager;
    }

    public LazyMultiModuleEngineFactory(Collection<Module> modules, IDependencyManager dependencyManager,
            Map<String, Object> externalParameters) {
        this(modules, dependencyManager);
        this.externalParameters = externalParameters;
    }

    public LazyMultiModuleEngineFactory(Collection<Module> modules, Class<T> interfaceClass) {
        this(modules);
        this.interfaceClass = interfaceClass;
    }

    public LazyMultiModuleEngineFactory(Collection<Module> modules, Class<T> interfaceClass,
            Map<String, Object> externalParameters) {
        this(modules, interfaceClass);
        this.externalParameters = externalParameters;
    }

    public LazyMultiModuleEngineFactory(Collection<Module> modules, IDependencyManager dependencyManager,
            Class<T> interfaceClass) {
        this(modules, dependencyManager);
        this.interfaceClass = interfaceClass;
    }

    public LazyMultiModuleEngineFactory(Collection<Module> modules, IDependencyManager dependencyManager,
            Class<T> interfaceClass, Map<String, Object> externalParameters) {
        this(modules, dependencyManager, interfaceClass);
        this.externalParameters = externalParameters;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }

    private void prepareOpenL() {
        LazyBinderInvocationHandler.setPrebindHandler(new IPrebindHandler() {

            @Override
            public IOpenMethod processMethodAdded(IOpenMethod method, XlsLazyModuleOpenClass moduleOpenClass) {
                return makeLazyMethod(method);
            }

            @Override
            public IOpenField processFieldAdded(IOpenField field, XlsLazyModuleOpenClass moduleOpenClass) {
                return makeLazyField(field);
            }
        });
    }

    private void restoreOpenL() {
        LazyBinderInvocationHandler.removePrebindHandler();
    }

    @SuppressWarnings({ "unchecked" })
    public Class<T> getInterfaceClass() {
        if (interfaceClass == null) {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            final String className = openClass.getName();
            try {
                interfaceClass = (Class<T>) interfaceClassGenerator.generateInterface(className, openClass,
                        getCompiledOpenClass().getClassLoader());
            } catch (Exception e) {
                String errorMessage = String.format("Failed to create interface : %s", className);
                if (log.isErrorEnabled()) {
                    log.error(errorMessage, e);
                }
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

            return prepareProxyInstance(openClassInstance, methodMap, runtimeEnv, getCompiledOpenClass()
                    .getClassLoader());
        } catch (Exception ex) {
            String errorMessage = "Can't instantiate engine instance";
            if (log.isErrorEnabled()) {
                log.error(errorMessage, ex);
            }
            throw new OpenlNotCheckedException(errorMessage, ex);
        }
    }

    /* package */Module getModuleForMember(IOpenMember member) {
        String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
        Module module = getModuleForSourceUrl(sourceUrl, modules);
        if (module != null) {
            return module;
        }

        DeploymentRelatedInfo deploymentRelatedInfo = DeploymentRelatedInfo.getCurrent();

        if (deploymentRelatedInfo != null) {
            module = getModuleForSourceUrl(sourceUrl, deploymentRelatedInfo.getModulesInDeployment());
            if (module != null) {
                return module;
            }
        }

        throw new OpenlNotCheckedException("Module not found");
    }

    private Module getModuleForSourceUrl(String sourceUrl, Collection<Module> modules) {
        for (Module module : modules) {
            String modulePath = module.getRulesRootPath().getPath();
            try {
                if (FilenameUtils.normalize(sourceUrl).equals(
                        FilenameUtils.normalize(new File(modulePath).getCanonicalFile().toURI().toURL()
                                .toExternalForm()))) {
                    return module;
                }
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to build url of module '" + module.getName() + "' with path: " + modulePath, e);
                }
            }
        }
        return null;
    }

    private LazyMethod makeLazyMethod(IOpenMethod method) {
        final Module declaringModule = getModuleForMember(method);
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        return new LazyMethod(method.getName(), argTypes, dependencyManager, true, Thread.currentThread()
                .getContextClassLoader(), method, externalParameters) {
            @Override
            public Module getModule(IRuntimeEnv env) {
                return declaringModule;
            }
        };
    }

    private LazyField makeLazyField(IOpenField field) {
        final Module declaringModule = getModuleForMember(field);
        return new LazyField(field.getName(), dependencyManager, true, Thread.currentThread().getContextClassLoader(),
                field, externalParameters) {
            @Override
            public Module getModule(IRuntimeEnv env) {
                return declaringModule;
            }
        };
    }

    private CompiledOpenClass initializeOpenClass() {
        // put prebinder to openl
        try {
            prepareOpenL();
            IOpenSourceCodeModule mainModule = createMainModule();
            RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(mainModule,
                    AOpenLEngineFactory.DEFAULT_USER_HOME, getOpenlName());// FIXME
            engineFactory.setDependencyManager(dependencyManager);
            engineFactory.setExecutionMode(true);
            CompiledOpenClass result = engineFactory.getCompiledOpenClass();
            postProcess(result.getOpenClassWithErrors());
            return result;
        } finally {
            restoreOpenL();
        }
    }

    private void postProcess(IOpenClass openClass) {
        ModuleOpenClass topOpenClass = (ModuleOpenClass) openClass;
        for (CompiledOpenClass dep : topOpenClass.getDependencies()) {
            for (IOpenMethod m : dep.getOpenClass().getMethods()) {
                if (m instanceof LazyMethod) {
                    LazyMethod lm = (LazyMethod) m;
                    lm.setTopModule(topOpenClass);
                }
            }
        }
    }

    private IOpenSourceCodeModule createMainModule() {
        List<IDependency> dependencies = new ArrayList<IDependency>();

        for (Module module : modules) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        params.put("external-dependencies", dependencies);
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
}
