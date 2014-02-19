package org.openl.rules.project.instantiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyManager;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

/**
 * Instantiation strategy that combines several modules into single rules
 * module.
 * 
 * Note: it works only in execution mode.
 * 
 * @author PUdalau
 * 
 */
public abstract class MultiModuleInstantiationStartegy extends CommonRulesInstantiationStrategy {
    private final Log log = LogFactory.getLog(MultiModuleInstantiationStartegy.class);

    private Collection<Module> modules;

    public MultiModuleInstantiationStartegy(Collection<Module> modules, IDependencyManager dependencyManager) {
        this(modules, dependencyManager, null);
    }

    public MultiModuleInstantiationStartegy(Collection<Module> modules, IDependencyManager dependencyManager,
            ClassLoader classLoader) {
        // multimodule is only available for execution(execution mode == true)
        super(true, dependencyManager != null ? dependencyManager : createDependencyManager(modules), classLoader);
        this.modules = modules;
    }

    private static IDependencyManager createDependencyManager(Collection<Module> modules) {
        RulesProjectDependencyManager multiModuleDependencyManager = new RulesProjectDependencyManager();
        // multimodule is only available for execution(execution mode == true)
        multiModuleDependencyManager.setExecutionMode(true);
        IDependencyLoader loader = new RulesModuleDependencyLoader(modules);
        multiModuleDependencyManager.setDependencyLoaders(Arrays.asList(loader));
        return multiModuleDependencyManager;
    }

    @Override
    public Collection<Module> getModules() {
        return modules;
    }

    @Override
    protected ClassLoader initClassLoader() throws RulesInstantiationException{
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(Thread.currentThread()
                .getContextClassLoader());
        for (Module module : modules) {
            try{
                CompiledDependency compiledDependency = getDependencyManager().loadDependency(
                    new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null)));
                CompiledOpenClass compiledOpenClass = compiledDependency.getCompiledOpenClass();
                classLoader.addClassLoader(compiledOpenClass.getClassLoader());
            }catch(OpenLCompilationException e){
                throw new RulesInstantiationException(e.getMessage(), e);
            }
        }
        return classLoader;
    }

    @Override
    public void setExternalParameters(Map<String, Object> parameters) {
        super.setExternalParameters(parameters);
        IDependencyManager dm = getDependencyManager();
        if (dm instanceof DependencyManager) {
            ((DependencyManager) dm).setExternalParameters(parameters);
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Can't set external parameters to dependency manager " + String.valueOf(dm));
            }
        }
    }

    /**
     * @return Special empty virtual {@link IOpenSourceCodeModule} with
     *         dependencies on all modules.
     */
    protected IOpenSourceCodeModule createVirtualSourceCodeModule() {
        List<IDependency> dependencies = new ArrayList<IDependency>();

        for (Module module : getModules()) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        if (params.get("external-dependencies") != null){
            @SuppressWarnings("unchecked")
            List<IDependency> externalDependencies = (List<IDependency>) params.get("external-dependencies");
            dependencies.addAll(externalDependencies);
        }
        params.put("external-dependencies", dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }
}
