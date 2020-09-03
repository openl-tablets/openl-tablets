package org.openl.rules.project.instantiation;

import java.util.*;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLCompileManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.Module;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

/**
 * Instantiation strategy that combines several modules into single rules module.
 * <p/>
 * Note: it works only in execution mode.
 *
 * @author PUdalau
 */
public abstract class MultiModuleInstantiationStartegy extends CommonRulesInstantiationStrategy {

    private Collection<Module> modules;

    public MultiModuleInstantiationStartegy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            boolean executionMode) {
        this(modules, dependencyManager, null, executionMode);
    }

    public MultiModuleInstantiationStartegy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode) {
        // multimodule is only available for execution(execution mode == true)
        super(executionMode, dependencyManager, classLoader);
        this.modules = modules;
    }

    @Override
    public Collection<Module> getModules() {
        return modules;
    }

    @Override
    protected ClassLoader initClassLoader() throws RulesInstantiationException {
        OpenLBundleClassLoader classLoader = new OpenLBundleClassLoader(Thread.currentThread().getContextClassLoader());
        for (Module module : modules) {
            try {
                CompiledDependency compiledDependency = getDependencyManager().loadDependency(
                    new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null)));
                CompiledOpenClass compiledOpenClass = compiledDependency.getCompiledOpenClass();
                classLoader.addClassLoader(compiledOpenClass.getClassLoader());
            } catch (OpenLCompilationException e) {
                throw new RulesInstantiationException(e.getMessage(), e);
            }
        }
        return classLoader;
    }

    /**
     * @return Special empty virtual {@link IOpenSourceCodeModule} with dependencies on all modules.
     */
    protected IOpenSourceCodeModule createVirtualSourceCodeModule() {
        List<IDependency> dependencies = new ArrayList<>();

        for (Module module : getModules()) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        if (params.get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY) != null) {
            @SuppressWarnings("unchecked")
            List<IDependency> externalDependencies = (List<IDependency>) params
                .get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY);
            dependencies.addAll(externalDependencies);
        }
        params.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }
}
