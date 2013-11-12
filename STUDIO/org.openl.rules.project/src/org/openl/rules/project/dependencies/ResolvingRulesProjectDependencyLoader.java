package org.openl.rules.project.dependencies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class ResolvingRulesProjectDependencyLoader implements IDependencyLoader {

    private RulesProjectResolver projectResolver;
    private RulesModuleDependencyLoader delegate;
    
    public ResolvingRulesProjectDependencyLoader(String workspace) {
        this(initResolverFromClasspath(workspace));
    }
    
    public ResolvingRulesProjectDependencyLoader(RulesProjectResolver projectResolver) {
        this.projectResolver = projectResolver;
        init();
    }

    public CompiledDependency load(String dependencyName, IDependencyManager dependencyManager) {
        return delegate.load(dependencyName, dependencyManager);
    }

    private void init() {

        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();
        Map<String, Module> modules = new HashMap<String, Module>();
 
        for (ProjectDescriptor project : projects) {
            for (Module module : project.getModules()) {
                String key = module.getName();
                modules.put(key, module);
            }
        }
        
        delegate = new RulesModuleDependencyLoader(modules.values());
    }
    
    private static RulesProjectResolver initResolverFromClasspath(String workspace) {

        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        projectResolver.setWorkspace(workspace);

        return projectResolver;
    }

}
