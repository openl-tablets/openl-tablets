package org.openl.source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.conf.IUserContext;
import org.openl.source.loaders.IDependencyLoader;
import org.openl.source.loaders.ModuleLoader;
import org.openl.syntax.code.DependencyType;

public abstract class DependencyManager implements IDependencyManager {
    
    private IUserContext userContext;
    private String openlName;
    
    private Map<DependencyType, IDependencyLoader> dependencyLoaders = new HashMap<DependencyType, IDependencyLoader>();
    
    public DependencyManager(IUserContext userContext, String openlName) {
        this.userContext = userContext;
        this.openlName = openlName;
        initLoaders();
    }
    
    public DependencyManager() {
        initLoaders();
    }
    
    private Map<IOpenSourceCodeModule, Set<IOpenSourceCodeModule>> dependencyMap = 
        new HashMap<IOpenSourceCodeModule, Set<IOpenSourceCodeModule>>();

    public boolean addDependenciesSources(IOpenSourceCodeModule moduleSource, Set<IOpenSourceCodeModule> dependentSources) {
        boolean result = false;
        if (!dependencyMap.containsKey(moduleSource) && dependentSources != null) {
            dependencyMap.put(moduleSource, new HashSet<IOpenSourceCodeModule>(dependentSources));
            result = true;
        } 
        return result;
    }

    public Set<IOpenSourceCodeModule> getDependenciesSources(IOpenSourceCodeModule moduleSource) {    
        Set<IOpenSourceCodeModule> sources = dependencyMap.get(moduleSource);
        if (sources != null) {
            return new HashSet<IOpenSourceCodeModule>(sources);
        } else {
            return new HashSet<IOpenSourceCodeModule>();
        }
    }
    
    public IOpenSourceCodeModule find(String dependency, String searchPath) {
        IDependencyLoader loader = getLoader(DependencyType.MODULE);
        return ((ModuleLoader)loader).find(dependency, searchPath);
    }
    
    // new functionality    
    public IOpenSourceCodeModule findDependencySource(String dependency, String rootFileUri, DependencyType dependencyType) {
        IDependencyLoader loader = getLoader(dependencyType);        
        IOpenSourceCodeModule dependencySource = loader.getDependencySource(dependency, rootFileUri);
        if (dependencySource != null) {
            return dependencySource;
        } else {
            // can`t get source for this dependency type. 
        }
        
        return dependencySource;
    }
    
    public CompiledOpenClass findCompiledDependency(String dependency, String rootFileUri, DependencyType dependencyType) {
        IDependencyLoader loader = getLoader(dependencyType);
        CompiledOpenClass compiledDependency = loader.getCompiledDependency(dependency, rootFileUri);
        if (compiledDependency != null) {
            return compiledDependency;
        } else {
         // can`t get compiled dependency for this dependency type.
        }
        return compiledDependency;
    }
    
    private IDependencyLoader getLoader(DependencyType dependencyType) {
        IDependencyLoader loader = dependencyLoaders.get(dependencyType);
        if (loader == null) {
            // dependency type is not supported
            // throw smth
        }
        return loader;
    }

    public IDependencyLoader getDependencyLoader(DependencyType dependencyType) {        
        return dependencyLoaders.get(dependencyType);
    }
    
    protected IUserContext getUserContext() {
        return userContext;
    }
    
    protected String getOpenlName() {
        return openlName;
    }
    
    protected Map<DependencyType, IDependencyLoader> getDependencyLoaders() {
        return this.dependencyLoaders;
    }
    
    protected abstract void initLoaders();

}
