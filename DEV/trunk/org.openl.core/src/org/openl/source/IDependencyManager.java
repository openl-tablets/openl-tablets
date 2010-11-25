package org.openl.source;

import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.source.loaders.IDependencyLoader;
import org.openl.syntax.code.DependencyType;


/**
 * Draft.
 * @author DLiauchuk
 *
 */
public interface IDependencyManager {
    
    IOpenSourceCodeModule find(String dependency, String searchPath);
    
    Set<IOpenSourceCodeModule> getDependenciesSources(IOpenSourceCodeModule moduleSource);
    
    boolean addDependenciesSources(IOpenSourceCodeModule moduleSource, Set<IOpenSourceCodeModule> dependentSources);
    
    // new functionality
    IOpenSourceCodeModule findDependencySource(String dependency, String rootFileUri, DependencyType dependencyType);
    
    CompiledOpenClass findCompiledDependency(String dependency, String rootFileUri, DependencyType dependencyType);
    
    IDependencyLoader getDependencyLoader(DependencyType dependencyType);

}