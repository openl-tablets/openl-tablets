package org.openl.source;

import java.util.Set;


/**
 * Draft.
 * @author DLiauchuk
 *
 */
public interface IDependencyManager {
    
    IOpenSourceCodeModule find(String dependency, String searchPath);

    void addSource(IOpenSourceCodeModule moduleSource, Set<IOpenSourceCodeModule> dependentSources);
        
    Set<IOpenSourceCodeModule> getDependentSources(IOpenSourceCodeModule moduleSource);

}