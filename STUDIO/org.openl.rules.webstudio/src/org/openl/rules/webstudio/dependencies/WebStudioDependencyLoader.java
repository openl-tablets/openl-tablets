package org.openl.rules.webstudio.dependencies;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;

final class WebStudioDependencyLoader implements IDependencyLoader {

    private final Log log = LogFactory.getLog(WebStudioDependencyLoader.class);

    private final String dependencyName;
    private final Collection<Module> modules;
    private volatile CompiledDependency compiledDependency = null;

    private final boolean singleModuleMode;

    WebStudioDependencyLoader(String dependencyName, Collection<Module> modules, boolean singleModuleMode) {
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName arg can't be null!");
        }
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("modules arg can't be null or empty!");
        }
        this.dependencyName = dependencyName;
        this.modules = modules;
        this.singleModuleMode = singleModuleMode;
    }

    @Override
    public CompiledDependency load(String dependencyName, IDependencyManager dm) throws OpenLCompilationException{
        WebStudioWorkspaceRelatedDependencyManager dependencyManager;
        if (dm instanceof WebStudioWorkspaceRelatedDependencyManager) {
            dependencyManager = (WebStudioWorkspaceRelatedDependencyManager) dm;
        } else {
            throw new IllegalStateException("This loader works only with WebStudioWorkspaceRelatedDependencyManager!");
        }

        if (this.dependencyName.equals(dependencyName)) {
            if (compiledDependency != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Dependency for dependencyName = " + dependencyName + " from cache was returned.");
                }
                return compiledDependency;
            }
            synchronized (dependencyManager) {
                try {
                    if (dependencyManager.getStack().contains(dependencyName)) {
                        OpenLMessagesUtils.addError("Circular dependency detected in module: " + dependencyName);
                        return null;
                    }
                    RulesInstantiationStrategy rulesInstantiationStrategy;
                    ClassLoader classLoader = dependencyManager.getClassLoader(modules);
                    if (modules.size() == 1) {
                        dependencyManager.getStack().add(dependencyName);
                        if (log.isDebugEnabled()) {
                            log.debug("Creating dependency for dependencyName = " + dependencyName);
                        }
                        rulesInstantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(modules.iterator()
                            .next(), false, dependencyManager, classLoader);
                    } else {
                        if (modules.size() > 1) {
                            rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                                dependencyManager,
                                classLoader);
                        } else {
                            throw new IllegalStateException("Modules collection can't be empty");
                        }
                    }
                    Map<String, Object> parameters = dependencyManager.getExternalParameters();
                    if (!singleModuleMode) {
                        parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(parameters,
                                modules);
                    }
                    rulesInstantiationStrategy.setExternalParameters(parameters);
                    rulesInstantiationStrategy.setServiceClass(EmptyInterface.class); // Prevent interface generation
                    try {
                        CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
                        CompiledDependency cd = new CompiledDependency(dependencyName, compiledOpenClass);
                        if (log.isDebugEnabled()) {
                            log.debug("Dependency for dependencyName = " + dependencyName + " was stored to cache.");
                        }
                        compiledDependency = cd;
                        return compiledDependency;
                    } catch (Exception ex) {
                        throw new OpenLCompilationException("Can't load dependency with name '" + dependencyName + "'.",
                            ex);
                    }
                } finally {
                    dependencyManager.getStack().pollLast();
                }
            }
        }
        return null;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public void reset() {
        compiledDependency = null;
    }

    public static interface EmptyInterface {
    }
}