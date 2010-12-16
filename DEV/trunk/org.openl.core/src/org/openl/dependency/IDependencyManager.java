package org.openl.dependency;

import org.openl.exception.OpenLCompilationException;
import org.openl.syntax.code.IDependency;

public interface IDependencyManager {
    
    /**
     * Load and compile the given dependency.
     *  
     * @param dependency to be loaded.
     * @return {@link CompiledDependency}
     * 
     * @throws OpenLCompilationException
     */
    CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException;
    
    /**
     * Remove given dependency from cache. 
     * 
     * @param dependency to be cleaned from cache.
     */
    void reset(IDependency dependency);
    
}