package org.openl.rules.ruleservice.publish.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.types.IOpenClass;

/**
 * Gathers info where each compiled moduleOpenClass located(modules).
 * 
 * @author PUdalau
 */
public class ModuleInfoGatheringDependencyLoader extends RulesModuleDependencyLoader {
    private ModuleStatistics moduleStatistics = new ModuleStatistics();

    public ModuleInfoGatheringDependencyLoader(Collection<Module> modules) {
        super(modules);
        this.moduleStatistics = new ModuleStatistics();
    }

    @Override
    public CompiledDependency load(String dependencyName, IDependencyManager dependencyManager) {
        CompiledDependency compiledDependency = super.load(dependencyName, dependencyManager);
        moduleStatistics.register(compiledDependency.getCompiledOpenClass().getOpenClass(),
                findDependencyModule(dependencyName));
        return compiledDependency;
    }

    public ModuleStatistics getModuleStatistic() {
        return moduleStatistics;
    }

    /**
     * Association between compiled module open classes and modules containing
     * them.
     */
    public static class ModuleStatistics {
        private Map<IOpenClass, Module> modules = new HashMap<IOpenClass, Module>();

        public void register(IOpenClass openClass, Module module) {
            modules.put(openClass, module);
        }

        public Map<IOpenClass, Module> getModules() {
            return modules;
        }
    }
}
