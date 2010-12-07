package org.openl.rules.project.dependencies;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoader;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
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
//                ClassLoader parentClassLoader = OpenLClassLoaderHelper.getContextClassLoader();
                OpenLClassLoader moduleClassLoader = new SimpleBundleClassLoader();
                OpenLClassLoaderHelper.extendClasspath(moduleClassLoader, urls);
                
                RulesInstantiationStrategy strategy = RulesInstantiationStrategyFactory.getStrategy(module, true, dependencyManager, moduleClassLoader);
                CompiledOpenClass compiledOpenClass = strategy.compile(ReloadType.NO);
                
                return new CompiledDependency(dependencyName, compiledOpenClass, moduleClassLoader);
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
