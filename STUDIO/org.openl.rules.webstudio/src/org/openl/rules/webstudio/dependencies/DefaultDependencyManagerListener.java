package org.openl.rules.webstudio.dependencies;

import org.openl.dependency.CompiledDependency;
import org.openl.syntax.code.IDependency;


public class DefaultDependencyManagerListener implements DependencyManagerListener {

    @Override
    public void onLoadDependency(CompiledDependency loadedDependency) {
        // Do nothing
    }

    @Override
    public void onResetDependency(IDependency dependency) {
        // Do nothing
    }
}
