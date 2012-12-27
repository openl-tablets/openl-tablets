package org.openl.rules.project.dependencies;

import java.util.ArrayList;
import java.util.List;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.syntax.code.IDependency;

/**
 * A dependency manager that uses other dependency managers (if any) to load a
 * dependent module. If delegates cannot load a dependency, then default
 * implementation of RulesProjectDependencyManager is used
 * 
 * @author NSamatov
 */
public class CompositeRulesProjectDependencyManager extends RulesProjectDependencyManager {
    private List<IDependencyManager> delegates = new ArrayList<IDependencyManager>();

    @Override
    protected CompiledDependency handleLoadDependency(IDependency dependency) throws OpenLCompilationException {
        for (IDependencyManager delegate : delegates) {
            CompiledDependency compiledDependency = delegate.loadDependency(dependency);
            if (compiledDependency != null) {
                return compiledDependency;
            }
        }

        return super.handleLoadDependency(dependency);
    }

    public void addDependencyManager(IDependencyManager delegate) {
        delegates.add(delegate);
    }

    public boolean removeDependencyManager(IDependencyManager delegate) {
        return delegates.remove(delegate);
    }

}
