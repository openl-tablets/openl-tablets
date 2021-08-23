package org.openl.dependency;

import java.util.Objects;

import org.openl.CompiledOpenClass;

/**
 * Simple bean that handles compiled dependency.
 *
 */
public class CompiledDependency {

    private final ResolvedDependency dependency;
    private final CompiledOpenClass compiledOpenClass;
    private final DependencyType dependencyType;

    public CompiledDependency(ResolvedDependency dependency,
            CompiledOpenClass compiledOpenClass,
            DependencyType dependencyType) {
        this.dependency = Objects.requireNonNull(dependency, "dependency cannot be null");
        this.compiledOpenClass = Objects.requireNonNull(compiledOpenClass, "compiledOpenClass cannot be null");
        this.dependencyType = Objects.requireNonNull(dependencyType, "dependencyType cannot be null");
    }

    public ResolvedDependency getDependency() {
        return dependency;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
    }

    public ClassLoader getClassLoader() {
        return getCompiledOpenClass().getClassLoader();
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }
}
