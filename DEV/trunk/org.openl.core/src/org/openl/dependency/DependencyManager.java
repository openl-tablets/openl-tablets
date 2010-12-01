package org.openl.dependency;

import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.syntax.code.IDependency;

public abstract class DependencyManager implements IDependencyManager {
    
    public CompiledDependency loadDependency(IDependency dependency) throws Exception {

        String dependencyName = dependency.getNode().getIdentifier();

        List<IDependencyLoader> dependencyLoaders = getDependencyLoaders();
        CompiledDependency compiledDependency = loadDependency(dependencyName, dependencyLoaders);

        if (compiledDependency == null) {
            throw new OpenLCompilationException(String.format("Dependency with name '%s' is not found", dependencyName), null, dependency.getNode().getSourceLocation());
        }

        return compiledDependency;
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
