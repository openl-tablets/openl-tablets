package org.openl.rules.webstudio.dependencies;

import org.openl.dependency.CompiledDependency;
import org.openl.syntax.code.IDependency;

public interface DependencyManagerListener {
    public void onLoadDependency(CompiledDependency loadedDependency);

    public void onResetDependency(IDependency dependency);
}
