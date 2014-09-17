package org.openl.rules.webstudio.dependencies;

import org.openl.dependency.CompiledDependency;
import org.openl.syntax.code.IDependency;

public interface DependencyManagerListener {
    void onLoadDependency(CompiledDependency loadedDependency);

    void onResetDependency(IDependency dependency);
}
