package org.openl.dependency;

import org.openl.CompiledOpenClass;

public class CompiledDependency {

    private String dependencyName;
    private CompiledOpenClass compiledOpenClass;
    private ClassLoader classLoader;
    
    public CompiledDependency(String dependencyName, CompiledOpenClass compiledOpenClass, ClassLoader classLoader) {
        this.dependencyName = dependencyName;
        this.compiledOpenClass = compiledOpenClass;
        this.classLoader = classLoader;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
}
