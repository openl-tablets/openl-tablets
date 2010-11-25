package org.openl.source.loaders;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;

/**
 * Abstract module dependency loader. 
 * 
 * @author DLiauchuk
 *
 */
public abstract class ModuleLoader implements IDependencyLoader {
    
    private IUserContext userContext;
    private String openlName;
    
    public ModuleLoader(IUserContext userContext, String openlName) {
        this.userContext = userContext;
        this.openlName = openlName;
    }
    
    public abstract IOpenSourceCodeModule find(String dependency, String rootFileUrl);
    
    protected CompiledOpenClass process(String dependency, String rootFileUrl) {
        IOpenSourceCodeModule dependencySource = find(dependency, rootFileUrl);
        CompiledOpenClass compiledDependency = OpenLManager.compileModuleWithErrors(OpenL.getInstance(openlName, userContext), dependencySource);
        return compiledDependency;
    }

    public CompiledOpenClass getCompiledDependency(String dependency, String rootFileUri) {        
        return process(dependency, rootFileUri);
    }

    public IOpenSourceCodeModule getDependencySource(String dependency, String rootFileUri) {
        return find(dependency, rootFileUri);
    }

    public boolean isSourceLoader() {        
        return false;
    }

}
