package org.openl.rules.project.instantiation;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectInstantiationManager {

    private ProjectDescriptor projectDescriptor;
    private Map<String, Module> modulesMap;

    public ProjectInstantiationManager(ProjectDescriptor projectDescriptor) {
        this.projectDescriptor = projectDescriptor;
        
        init();
    }
    
    private void init() {
        modulesMap = RulesProjectHelper.makeModulesMap(projectDescriptor.getModules());
    }
    
    public CompiledOpenClass loadModule(String moduleName) {
        Module module = modulesMap.get(moduleName);
        
        return loadModule(module);
    }
    
    public CompiledOpenClass loadModule(Module module) {
        return null;
    }
    
}
