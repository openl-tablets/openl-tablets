package org.openl.source;

import java.util.Set;


/**
 * Draft.
 * @author DLiauchuk
 *
 */
public interface IDependencyManager {
    
    IOpenSourceCodeModule find(String dependency, String searchPath);

    boolean addDependenciesSources(IOpenSourceCodeModule moduleSource, Set<IOpenSourceCodeModule> dependentSources);
        
    Set<IOpenSourceCodeModule> getDependenciesSources(IOpenSourceCodeModule moduleSource);

}