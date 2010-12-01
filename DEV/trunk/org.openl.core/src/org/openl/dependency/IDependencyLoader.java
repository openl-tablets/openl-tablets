package org.openl.dependency;


public interface IDependencyLoader {

    CompiledDependency load(String dependencyName, IDependencyManager dependencyManager);
}
