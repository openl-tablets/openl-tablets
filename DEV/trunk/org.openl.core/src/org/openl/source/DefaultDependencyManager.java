package org.openl.source;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.PathTool;

/**
 * Draft. Searches for dependencies in root module path.
 * @author DLiauchuk
 *
 */
public class DefaultDependencyManager implements IDependencyManager {
    
    private OpenL openl;
    
    /**
     * uri to the module folder
     */
    private String moduleUri;
    
    public DefaultDependencyManager(String openlName, IUserContext ucxt, String moduleUri) {
        this(OpenL.getInstance(openlName, ucxt), moduleUri);
    }
    
    public DefaultDependencyManager(OpenL openl, String moduleUri) {
        this.openl = openl;
        this.moduleUri = moduleUri;
    }
    
    private List<CompiledOpenClass> dependencies = new ArrayList<CompiledOpenClass>();

    public List<CompiledOpenClass> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<CompiledOpenClass> dependencies) {
        this.dependencies = dependencies;
    }
    
    public void addDependency(CompiledOpenClass dependency) {
        if (!dependencies.contains(dependency)) {
            dependencies.add(dependency);
        }
    }
    
    public void process(IOpenSourceCodeModule dependencySource) {
        CompiledOpenClass depOpenClass = OpenLManager.compileModuleWithErrors(openl, dependencySource);        
        addDependency(depOpenClass);
    }
    
    /**
     * Searches dependencies in root module path.
     */
    public IOpenSourceCodeModule find(String dependency) {
        IOpenSourceCodeModule dependencySource = null;
        try {
            String newURL = PathTool.mergePath(moduleUri, dependency);
            dependencySource = new URLSourceCodeModule(new URL(newURL));
        } catch (Throwable t) {
            // can`t find dependency
            // do smth
        }
        return dependencySource;
    }
    

}
