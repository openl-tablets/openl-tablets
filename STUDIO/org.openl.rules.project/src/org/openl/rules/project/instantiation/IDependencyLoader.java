package org.openl.rules.project.instantiation;

import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.ProjectDescriptor;

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
    CompiledDependency getCompiledDependency() throws OpenLCompilationException;

    String getDependencyName();

    ProjectDescriptor getProject();

    boolean isProject();

    void reset();

}
