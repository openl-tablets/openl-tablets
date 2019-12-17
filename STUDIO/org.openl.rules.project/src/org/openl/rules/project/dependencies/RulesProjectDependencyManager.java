package org.openl.rules.project.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.dependency.DependencyManager;
import org.openl.dependency.loader.IDependencyLoader;

public class RulesProjectDependencyManager extends DependencyManager {

    private List<IDependencyLoader> loaders = new ArrayList<>();

    @Override
    public List<IDependencyLoader> getDependencyLoaders() {
        return loaders;
    }

    public void setDependencyLoaders(List<IDependencyLoader> loaders) {
        this.loaders = loaders;
    }

    @Override
    public Collection<String> getAllDependencies() {
        return null;
    }

    @Override
    public boolean isEmptyDependencyCompilationStack() {
        return true;
    }
}
