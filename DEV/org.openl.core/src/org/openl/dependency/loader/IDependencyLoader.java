package org.openl.dependency.loader;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;

/**
 * Common interface for all dependency loaders.<br>
 *
 */
public interface IDependencyLoader {

    /**
     * Finds the dependency and loads it by OpenL.
     *
     * @param dependencyName {@link String} dependency name
     * @param dependencyManager is used to load child dependencies for given one.
     * @return {@link CompiledDependency}
     */
    CompiledDependency load(String dependencyName,
            IDependencyManager dependencyManager) throws OpenLCompilationException;
}
