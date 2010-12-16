package org.openl.dependency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.syntax.code.IDependency;

public abstract class DependencyManager implements IDependencyManager {
    
    private Map<String, CompiledDependency> compiledDependencies = new HashMap<String, CompiledDependency>();  
    
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {

        String dependencyName = dependency.getNode().getIdentifier();

        if (compiledDependencies.containsKey(dependencyName)) {
            return compiledDependencies.get(dependencyName);
        }
        
        List<IDependencyLoader> dependencyLoaders = getDependencyLoaders();
        CompiledDependency compiledDependency = loadDependency(dependencyName, dependencyLoaders);

        if (compiledDependency == null) {
            throw new OpenLCompilationException(String.format("Dependency with name '%s' is not found", dependencyName), 
                null, dependency.getNode().getSourceLocation());
        }   
        
        compiledDependencies.put(dependencyName, compiledDependency);

        return compiledDependency;
    }
    
    public void reset(IDependency dependency) {
        String dependencyName = dependency.getNode().getIdentifier();
        if (compiledDependencies.containsKey(dependencyName)) {
            compiledDependencies.remove(dependencyName);
        }
    }

    public abstract List<IDependencyLoader> getDependencyLoaders();
    
    private CompiledDependency loadDependency(String dependencyName, List<IDependencyLoader> loaders) {
        
        for (IDependencyLoader loader : loaders) {
            CompiledDependency dependency = loader.load(dependencyName, this);
            
            if (dependency != null) {
                return dependency;
            }
        }
        
        return null;
    }
    
}
