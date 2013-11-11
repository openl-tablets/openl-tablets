package org.openl.rules.ruleservice.core;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.lazy.LazyBinderInvocationHandler;
import org.openl.rules.ruleservice.publish.lazy.LazyInstantiationStrategy;

final class LazyRuleServiceDependencyLoader implements IDependencyLoader {
    
    private final Log log = LogFactory.getLog(LazyRuleServiceDependencyLoader.class);
    
    private final String name;
    private final DeploymentDescription deployment;
    private final Collection<Module> modules;
    private volatile CompiledDependency lazyCompiledDependency = null;
    private boolean flagThatThisDependencyNameInCompilationState = false;

    LazyRuleServiceDependencyLoader(DeploymentDescription deployment, String dependencyName,
            Collection<Module> modules) {
        if (deployment == null) {
            throw new IllegalArgumentException("deployment arg can't be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName arg can't be null!");
        }
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("modules arg can't be null or empty!");
        }
        this.name = dependencyName;
        this.deployment = deployment;
        this.modules = modules;
    }

    @Override
    public CompiledDependency load(String dependencyName, IDependencyManager dm) {
        RuleServiceDeploymentRelatedDependencyManager dependencyManager = null;
        if (dm instanceof RuleServiceDeploymentRelatedDependencyManager){
            dependencyManager = (RuleServiceDeploymentRelatedDependencyManager) dm;
        }else{
            throw new IllegalStateException("This loader works only with RuleServiceDeploymentRelatedDependencyManager!");
        }
        
        if (name.equals(dependencyName)) {
            boolean isLazyCompilation = true;
            IPrebindHandler prebindHandler = LazyBinderInvocationHandler.getPrebindHandler();
            if (lazyCompiledDependency != null && RuleServiceDeploymentRelatedDependencyManager.getCurrentClassCompilation() != lazyCompiledDependency) {
                if (log.isDebugEnabled()) {
                    log.debug("Lazy dependency for dependencyName = " + dependencyName + " from cache was returned.");
                }
                return lazyCompiledDependency;
            }
            synchronized (dependencyManager) {
                try {
                    if (dependencyManager.getStack().contains(dependencyName)) {
                        OpenLMessagesUtils.addError("Circular dependency detected in module: " + dependencyName);
                        return null;
                    }
                    RulesInstantiationStrategy rulesInstantiationStrategy = null;
                    ClassLoader classLoader = dependencyManager.getClassLoader(modules);
                    if (lazyCompiledDependency == null && !flagThatThisDependencyNameInCompilationState) {
                        flagThatThisDependencyNameInCompilationState = true;
                        if (modules.size() > 1) {
                            dependencyManager.getStack().add(dependencyName);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Creating lazy dependency for dependencyName = " + dependencyName);
                        }
                        rulesInstantiationStrategy = new LazyInstantiationStrategy(modules,
                            dependencyManager,
                            classLoader);
                    } else {
                        if (modules.size() == 1) {
                            CompiledDependency compiledDependency = CompiledDependencyCache.getInstance()
                                .get(deployment, dependencyName);
                            if (compiledDependency != null) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Actual dependency for dependencyName = " + dependencyName + " was returned from cache.");
                                }
                                return compiledDependency;
                            }
                            dependencyManager.getStack().add(dependencyName);
                            isLazyCompilation = false;
                            if (log.isDebugEnabled()) {
                                log.debug("Creating actual dependency for dependencyName = " + dependencyName);
                            }
                            rulesInstantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(modules.iterator()
                                .next(),
                                true,
                                dependencyManager,
                                classLoader);
                        } else {
                            throw new IllegalStateException("Illegal State!");
                        }
                    }
                    rulesInstantiationStrategy.setServiceClass(LazyRuleServiceDependencyLoaderInterface.class);//Prevent generation interface and Virtual module dublicate (instantiate method). Improve performance.
                    Map<String, Object> parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(), modules);
                    rulesInstantiationStrategy.setExternalParameters(parameters);
                    try {
                        LazyBinderInvocationHandler.removePrebindHandler();
                        CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
                        CompiledDependency compiledDependency = new CompiledDependency(dependencyName,
                            compiledOpenClass);
                        if (isLazyCompilation == false) {
                            if (log.isDebugEnabled()) {
                                log.debug("Actual dependency for dependencyName = " + dependencyName + " was stored to cache.");
                            }
                            CompiledDependencyCache.getInstance().putToCache(deployment,
                                dependencyName,
                                compiledDependency);
                        } else {
                            if (lazyCompiledDependency == null) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Lazy dependency for dependencyName = " + dependencyName + " was stored to cache.");
                                }
                                lazyCompiledDependency = compiledDependency;
                            } else {
                                throw new IllegalStateException("Should be compiled once!");
                            }
                        }
                        return compiledDependency;
                    } catch (Exception ex) {
                        OpenLMessagesUtils.addError("Can't load dependency " + dependencyName + ".");
                    } finally {
                        LazyBinderInvocationHandler.setPrebindHandler(prebindHandler);
                    }
                } finally {
                    if (isLazyCompilation == false) {
                        dependencyManager.getStack().pollLast();
                    }
                }
            }
        }
        return null;
    }
    
    public static interface LazyRuleServiceDependencyLoaderInterface{
    }
}