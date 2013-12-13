package org.openl.rules.webstudio.dependencies;

import org.openl.syntax.code.IDependency;

public interface DependencyManagerListener {
    public void onLoadDependency(IDependency dependency);

    public void onResetDependency(IDependency dependency);
}
