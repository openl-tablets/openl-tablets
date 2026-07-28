package org.openl.dependency;

import java.util.Objects;

import lombok.Getter;

import org.openl.CompiledOpenClass;

/**
 * Simple bean that handles compiled dependency.
 */
public class CompiledDependency {

    @Getter
    private final ResolvedDependency dependency;
    @Getter
    private final CompiledOpenClass compiledOpenClass;
    @Getter
    private final DependencyType dependencyType;

    public CompiledDependency(ResolvedDependency dependency,
                              CompiledOpenClass compiledOpenClass,
                              DependencyType dependencyType) {
        this.dependency = Objects.requireNonNull(dependency, "dependency cannot be null");
        this.compiledOpenClass = Objects.requireNonNull(compiledOpenClass, "compiledOpenClass cannot be null");
        this.dependencyType = Objects.requireNonNull(dependencyType, "dependencyType cannot be null");
    }

    public ClassLoader getClassLoader() {
        return getCompiledOpenClass().getClassLoader();
    }
}
