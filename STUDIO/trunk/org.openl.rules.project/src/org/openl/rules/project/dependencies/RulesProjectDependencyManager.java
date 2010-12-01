package org.openl.rules.project.dependencies;

import java.util.ArrayList;
import java.util.List;

import org.openl.dependency.DependencyManager;
import org.openl.dependency.IDependencyLoader;

public class RulesProjectDependencyManager extends DependencyManager {

    private List<IDependencyLoader> loaders = new ArrayList<IDependencyLoader>();
    
    @Override
    public List<IDependencyLoader> getDependencyLoaders() {
        return loaders;
    }

    public void setDependencyLoaders(List<IDependencyLoader> loaders) {
        this.loaders = loaders;
    }
    
}
