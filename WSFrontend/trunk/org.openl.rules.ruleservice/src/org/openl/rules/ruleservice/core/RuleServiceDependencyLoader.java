package org.openl.rules.ruleservice.core;

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
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;

final class RuleServiceDependencyLoader implements IDependencyLoader {

    private final Log log = LogFactory.getLog(RuleServiceDependencyLoader.class);

    private final String name;
    private final Collection<Module> modules;
    private volatile CompiledDependency compiledDependency = null;

    RuleServiceDependencyLoader(String dependencyName, Collection<Module> modules) {
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName arg can't be null!");
        }
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("modules arg can't be null or empty!");
        }
        this.name = dependencyName;
        this.modules = modules;
    }

    @Override
    public CompiledDependency load(String dependencyName, IDependencyManager dm) throws OpenLCompilationException{
        RuleServiceDeploymentRelatedDependencyManager dependencyManager = null;
        if (dm instanceof RuleServiceDeploymentRelatedDependencyManager) {
            dependencyManager = (RuleServiceDeploymentRelatedDependencyManager) dm;
        } else {
            throw new IllegalStateException("This loader works only with RuleServiceDeploymentRelatedDependencyManager!");
        }

        if (name.equals(dependencyName)) {
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
                    RulesInstantiationStrategy rulesInstantiationStrategy = null;
                    ClassLoader classLoader = dependencyManager.getClassLoader(modules);
                    if (modules.size() == 1) {
                        dependencyManager.getStack().add(dependencyName);
                        if (log.isDebugEnabled()) {
                            log.debug("Creating dependency for dependencyName = " + dependencyName);
                        }
                        rulesInstantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(modules.iterator()
                            .next(), true, dependencyManager, classLoader);
                    } else {
                        if (modules.size() > 1) {
                            rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                                dependencyManager,
                                classLoader);
                        } else {
                            throw new IllegalStateException("Illegal State!");
                        }
                    }
                    Map<String, Object> parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(),
                        modules);
                    rulesInstantiationStrategy.setExternalParameters(parameters);
                    rulesInstantiationStrategy.setServiceClass(RuleServiceDependencyLoaderInterface.class);//Prevent generation interface
                    try {
                        CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
                        CompiledDependency cd = new CompiledDependency(dependencyName, compiledOpenClass);
                        if (log.isDebugEnabled()) {
                            log.debug("Dependency for dependencyName = " + dependencyName + " was stored to cache.");
                        }
                        compiledDependency = cd;
                        return compiledDependency;
                    } catch (Exception ex) {
                        OpenLMessagesUtils.addError("Can't load dependency " + dependencyName + ".");
                    }
                } finally {
                    dependencyManager.getStack().pollLast();
                }
            }
        }
        return null;
    }
    public static interface RuleServiceDependencyLoaderInterface{
    }
}