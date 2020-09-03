package org.openl.rules.ruleservice.core;

import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager.DependencyCompilationType;

final class RuleServiceDependencyLoader extends SimpleDependencyLoader {

    public RuleServiceDependencyLoader(ProjectDescriptor project,
            Module module,
            RuleServiceDependencyManager dependencyManager) {
        super(project, module, false, true, dependencyManager);
    }

    @Override
    protected CompiledDependency compileDependency(String dependencyName,
            AbstractDependencyManager dependencyManager) throws OpenLCompilationException {
        if (dependencyManager instanceof RuleServiceDependencyManager) {
            RuleServiceDependencyManager ruleServiceDeploymentRelatedDependencyManager = (RuleServiceDependencyManager) dependencyManager;
            ruleServiceDeploymentRelatedDependencyManager.compilationBegin(this);
            CompiledDependency compiledDependency = null;
            try {
                compiledDependency = super.compileDependency(dependencyName, dependencyManager);
                ruleServiceDeploymentRelatedDependencyManager.compilationCompleted(this,
                    DependencyCompilationType.NONLAZY,
                    !compiledDependency.getCompiledOpenClass().hasErrors());
                return compiledDependency;
            } finally {
                if (compiledDependency == null) {
                    ruleServiceDeploymentRelatedDependencyManager
                        .compilationCompleted(this, DependencyCompilationType.NONLAZY, false);
                }
            }
        } else {
            return super.compileDependency(dependencyName, dependencyManager);
        }
    }
}