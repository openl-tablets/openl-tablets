package org.openl.dependency;

import org.openl.syntax.code.IDependency;

public interface IDependencyManager {
    
    CompiledDependency loadDependency(IDependency dependency) throws Exception;
    
}