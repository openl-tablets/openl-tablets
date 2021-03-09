package org.openl.rules.project.instantiation;

import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Common interface for all dependency loaders.<br>
 *
 */
public interface IDependencyLoader {

    /**
     * Finds the dependency and loads it by OpenL.
     *
     * @return {@link CompiledDependency}
     */
    CompiledDependency getCompiledDependency() throws OpenLCompilationException;

    CompiledDependency getRefToCompiledDependency();

    String getDependencyName();

    ProjectDescriptor getProject();

    Module getModule();

    boolean isProjectLoader();

    void reset();

}
