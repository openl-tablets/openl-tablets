package org.openl.rules.project.dependencies;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.OpenLClassLoaderHelper;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class RulesProjectDependencyLoader implements IDependencyLoader {

    private String workspace;
    private Map<String, Module> modules = new HashMap<String, Module>();
    
    public RulesProjectDependencyLoader(String workspace) {
        this.workspace = workspace;
        
        init();
    }

    private void init() {
        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        projectResolver.setWorkspace(workspace);
        
        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();

        for (ProjectDescriptor project : projects) {
            for (Module module : project.getModules()) {
                String key = module.getName();
                modules.put(key, module);
            }
        }
    }
    
    public CompiledDependency load(String dependencyName, IDependencyManager dependencyManager) {

        Module module = findDependencyModule(dependencyName);
        
        if(module != null) {
     
            try {
                URL[] urls = module.getProject().getClassPathUrls();
                URLClassLoader currentClassLoader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
                OpenLClassLoaderHelper.extendClasspath(currentClassLoader, urls);
                
                RulesInstantiationStrategy strategy = RulesInstantiationStrategyFactory.getStrategy(module, true, dependencyManager, currentClassLoader);
                // set to strategy new class loader.
                
                CompiledOpenClass compiledOpenClass = strategy.compile(ReloadType.NO);
                
                return new CompiledDependency(dependencyName, compiledOpenClass, Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                throw new RuntimeException(String.format("Cannot load dependency '%s'", dependencyName) , e);
            }
        }

        return null;
    }
    
    private Module findDependencyModule(String moduleName){
        return modules.get(moduleName);
    }
    
}
