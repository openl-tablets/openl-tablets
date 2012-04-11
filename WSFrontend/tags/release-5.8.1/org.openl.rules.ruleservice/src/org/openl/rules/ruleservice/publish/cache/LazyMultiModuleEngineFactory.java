package org.openl.rules.ruleservice.publish.cache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.rules.lang.xls.XlsPreBinder;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.cache.ModuleInfoGatheringDependencyLoader.ModuleStatistics;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;
import org.openl.rules.runtime.RulesFactory;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.types.OpenMethodDispatcher;
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
 * @author PUdalau
 */
public class LazyMultiModuleEngineFactory extends AOpenLEngineFactory {

    private static final Log LOG = LogFactory.getLog(LazyMultiModuleEngineFactory.class);

    private static final String RULES_XLS_OPENL_NAME = "org.openl.xls";

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;
    private Collection<Module> modules;
    private IDependencyManager dependencyManager;
    private ModuleStatistics moduleStatistic;

    public LazyMultiModuleEngineFactory(Collection<Module> modules) {
        super(RULES_XLS_OPENL_NAME);
        this.modules = modules;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }

        return compiledOpenClass;
    }

    private IOpenBinder previousBinder;

    private void prepareOpenL() {
        OpenL openL = getOpenL();
        previousBinder = openL.getBinder();
        openL.setBinder(new XlsPreBinder(getUserContext()));
    }

    private void restoreOpenL() {
        getOpenL().setBinder(previousBinder);
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass == null) {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            String className = openClass.getName();

            try {
                interfaceClass = RulesFactory.generateInterface(className, openClass, getCompiledOpenClass()
                        .getClassLoader());
            } catch (Exception e) {
                String errorMessage = String.format("Failed to create interface : %s", className);
                LOG.error(errorMessage, e);
                throw new OpenlNotCheckedException(errorMessage, e);
            }
        }
        return interfaceClass;
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { interfaceClass, IEngineWrapper.class };
    }

    @Override
    protected ThreadLocal<IRuntimeEnv> initRuntimeEnvironment() {
        return new ThreadLocal<org.openl.vm.IRuntimeEnv>() {
            @Override
            protected org.openl.vm.IRuntimeEnv initialValue() {
                return getOpenL().getVm().getRuntimeEnv();
            }
        };
    }

    @Override
    public Object makeInstance() {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();

            Object openClassInstance = openClass.newInstance(getRuntimeEnv());
            Map<Method, IOpenMember> methodMap = makeMethodMap(getInterfaceClass(), openClass);

            return makeEngineInstance(openClassInstance, methodMap, getRuntimeEnv(), getCompiledOpenClass()
                    .getClassLoader());
        } catch (Exception ex) {
            String errorMessage = "Cannot instantiate engine instance";
            LOG.error(errorMessage, ex);
            throw new OpenlNotCheckedException(errorMessage, ex);
        }
    }

    @Override
    protected Map<Method, IOpenMember> makeMethodMap(Class<?> engineInterface, IOpenClass moduleOpenClass) {
        Map<Method, IOpenMember> methodMap = super.makeMethodMap(engineInterface, moduleOpenClass);
        Map<Method, IOpenMember> processedMethodMap = new HashMap<Method, IOpenMember>();
        // it is necessary to reset dependency manager to clean up all cached
        // dependencies that was compiled in lazy mode
        dependencyManager.resetAll();
        for (Entry<Method, IOpenMember> entry : methodMap.entrySet()) {
            if (entry.getValue() instanceof OpenMethodDispatcher) {
                LazyMethodDispatcher newDispatcher = makeLazyMethodDispatcher((OpenMethodDispatcher) entry.getValue());
                processedMethodMap.put(entry.getKey(), newDispatcher);
            } else {
                if (entry.getValue() instanceof IOpenMethod) {
                    LazyMethod lazyMethod = makeLazyMethod((IOpenMethod) entry.getValue());
                    processedMethodMap.put(entry.getKey(), lazyMethod);
                } else if (entry.getValue() instanceof IOpenField) {
                    LazyField lazyField = makeLazyField((IOpenField) entry.getValue());
                    processedMethodMap.put(entry.getKey(), lazyField);
                } else {
                    LOG.warn(String.format("Unknown IOpenMember type in method map : %s", entry.getValue().getClass()
                        .getName()));
                }
            }
        }
        return processedMethodMap;
    }

    private LazyMethodDispatcher makeLazyMethodDispatcher(OpenMethodDispatcher dispatcher) {
        LazyMethodDispatcher newDispatcher = new LazyMethodDispatcher(dispatcher.getMethod(), null);
        for (IOpenMethod method : dispatcher.getCandidates()) {
            newDispatcher.addMethod(method, makeLazyMethod(method));
        }
        return newDispatcher;
    }

    private LazyMethod makeLazyMethod(IOpenMethod method) {
        Module declaringModule = moduleStatistic.getModules().get(method.getDeclaringClass());
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        return new LazyMethod(method.getName(), argTypes, declaringModule, dependencyManager, true, getCompiledOpenClass().getClassLoader());
    }

    private LazyField makeLazyField(IOpenField field) {
        Module declaringModule = moduleStatistic.getModules().get(field.getDeclaringClass());
        return new LazyField(field.getName(), declaringModule, dependencyManager, true, getCompiledOpenClass()
            .getClassLoader());
    }

    private CompiledOpenClass initializeOpenClass() {
        // put prebinder to openl
        prepareOpenL();
        IOpenSourceCodeModule mainModule = createMainModule();
        ApiBasedRulesEngineFactory factory = new ApiBasedRulesEngineFactory(RULES_XLS_OPENL_NAME, mainModule);
        factory.setDependencyManager(dependencyManager);
        factory.setExecutionMode(true);

        CompiledOpenClass result = factory.getCompiledOpenClass();
        restoreOpenL();
        return result;
    }

    private IOpenSourceCodeModule createMainModule() {
        List<IDependency> dependencies = new ArrayList<IDependency>();

        for (Module module : modules) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("external-dependencies", dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }

    public void setModuleStatistic(ModuleStatistics moduleStatistic) {
        this.moduleStatistic = moduleStatistic;
    }

}
