package org.openl.dependency;

import org.openl.CompiledOpenClass;

/**
 * Simple bean that handles compiled dependency.
 * 
 */
public class CompiledDependency {

    private String dependencyName;
    private CompiledOpenClass compiledOpenClass;

    public CompiledDependency(String dependencyName, CompiledOpenClass compiledOpenClass) {
        this.dependencyName = dependencyName;
        this.compiledOpenClass = compiledOpenClass;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
    }

    public ClassLoader getClassLoader() {
        return getCompiledOpenClass().getClassLoader();
    }

}
