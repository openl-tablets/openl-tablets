package org.openl.rules.project;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;


/**
 * Caches instantiation strategies for Modules.
 * 
 * @author PUdalau
 */
public class ModulesCache {
    @SuppressWarnings("unchecked")
    /**
     * Memory-sensitive cache.
     */
    private Map<Module, SingleModuleInstantiationStrategy> moduleInstantiators = new ReferenceMap(AbstractReferenceMap.WEAK,
            AbstractReferenceMap.SOFT);

    /**
     * Cache of dependent modules usages.
     * Key is dependent module name, value is a set of modules that depend on that module.
     */
    private Map<String, Set<Module>> dependencyUsages = new HashMap<String, Set<Module>>();

    public Set<Module> getModules() {
        return moduleInstantiators.keySet();
    }

    /**
     * Gets cached instantiation strategy for the module or creates it in cache.
     * 
     * @param module Module
     * @return Instantiation strategy for the module.
     */
    public SingleModuleInstantiationStrategy getInstantiationStrategy(Module module) {
        SingleModuleInstantiationStrategy strategy = moduleInstantiators.get(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module);
            moduleInstantiators.put(module, strategy);
        }
        return strategy;
    }
    
    public SingleModuleInstantiationStrategy getInstantiationStrategy(Module module, IDependencyManager dependencyManager) {
        SingleModuleInstantiationStrategy strategy = moduleInstantiators.get(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, dependencyManager);
            moduleInstantiators.put(module, strategy);
        }
        return strategy;
    }
   
    public SingleModuleInstantiationStrategy getInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        SingleModuleInstantiationStrategy strategy = moduleInstantiators.get(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, executionMode, dependencyManager);
            moduleInstantiators.put(module, strategy);
        }
        return strategy;
    }
   
    /**
     * Removes cached instantiation strategy for the module.
     * 
     * @param module Module
     */
    public void removeCachedModule(Module module) {
        moduleInstantiators.remove(module);
        removeModuleDependencies(module);
    }

    /**
     * Removes all cached instantiation strategies for the specified project.
     * 
     * @param project ProjectDescriptor
     */
    public void removeCachedProject(ProjectDescriptor project) {
        for (Module module : project.getModules()) {
            removeCachedModule(module);
        }
    }

    /**
     * Removes all cached instantiation strategies.
     */
    public void reset() {
        moduleInstantiators.clear();
        dependencyUsages.clear();
    }

    /**
     * Wrap original dependency manager to a proxy that will collect modules usages information
     * 
     * @param original original dependency manager
     * @param mainModule main module that will load its dependencies
     * @return wrapped dependency manager
     */
    public IDependencyManager wrapToCollectDependencies(IDependencyManager original, Module mainModule) {
        return (IDependencyManager) Proxy.newProxyInstance(original.getClass().getClassLoader(),
                new Class[] { IDependencyManager.class }, new DependenciesCollectingHandler(original, mainModule));
    }

    private void removeModuleDependencies(Module module) {
        Collection<Module> modulesUsingTheModule = dependencyUsages.get(module.getName());
        if (modulesUsingTheModule != null) {
            for (Module parentModule : new HashSet<Module>(modulesUsingTheModule)) {
                removeCachedModule(parentModule);
            }
            dependencyUsages.remove(module.getName());
        }
        for (Collection<Module> modules : dependencyUsages.values()) {
            modules.remove(module);
        }
    }

    private class DependenciesCollectingHandler implements InvocationHandler {
        private final IDependencyManager original;
        private final Module mainModule;

        public DependenciesCollectingHandler(IDependencyManager original, Module mainModule) {
            this.original = original;
            this.mainModule = mainModule;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("loadDependency")) {
                onLoadDependency(mainModule, (IDependency) args[0]);
            } else if (method.getName().equals("reset")) {
                onResetDependency(original, (IDependency) args[0]);
            }
            return method.invoke(original, args);
        }

        private void onLoadDependency(final Module mainModule, IDependency dependency) {
            if (dependency.getType() == DependencyType.MODULE) {
                Set<Module> modules = dependencyUsages.get(dependency.getNode().getIdentifier());
                if (modules == null) {
                    modules = Collections.newSetFromMap(new WeakHashMap<Module, Boolean>());
                    dependencyUsages.put(dependency.getNode().getIdentifier(), modules);
                }
                modules.add(mainModule);
            }
        }

        private void onResetDependency(final IDependencyManager original, IDependency dependency) {
            Collection<Module> modulesUsingTheModule = dependencyUsages.get(dependency.getNode().getIdentifier());
            if (modulesUsingTheModule != null) {
                for (Module module : modulesUsingTheModule) {
                    original.reset(new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module
                            .getName(), null)));
                }
            }
        }
    }
}
