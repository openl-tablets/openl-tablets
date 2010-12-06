package org.openl.dependency.loader;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;


public interface IDependencyLoader {

    CompiledDependency load(String dependencyName, IDependencyManager dependencyManager);
}
