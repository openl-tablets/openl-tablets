package org.openl.rules.ruleservice.core;

import java.util.Collection;
import java.util.Collections;

import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

final class RuleServiceDependencyLoader extends SimpleDependencyLoader {

    private RuleServiceDependencyLoader(String dependencyName,
            Collection<Module> modules,
            ProjectDescriptor projectDesciptor,
            RuleServiceDeploymentRelatedDependencyManager dependencyManager) {
        super(dependencyName, modules, false, true, projectDesciptor, dependencyManager);
    }

    public static RuleServiceDependencyLoader forModule(Module module,
            RuleServiceDeploymentRelatedDependencyManager dependencyManager) {
        return new RuleServiceDependencyLoader(module.getName(),
            Collections.singletonList(module),
            null,
            dependencyManager);
    }

    public static RuleServiceDependencyLoader forProject(ProjectDescriptor project,
            RuleServiceDeploymentRelatedDependencyManager dependencyManager) {
        return new RuleServiceDependencyLoader(ProjectExternalDependenciesHelper
            .buildDependencyNameForProject(project.getName()), project.getModules(), project, dependencyManager);
    }

    @Override
    protected CompiledDependency compileDependency(String dependencyName,
            AbstractDependencyManager dependencyManager) throws OpenLCompilationException {
        if (dependencyManager instanceof CompilationTimeLoggingDependencyManager) {
            CompilationTimeLoggingDependencyManager compilationTimeLoggingDependencyManager = (CompilationTimeLoggingDependencyManager) dependencyManager;
            compilationTimeLoggingDependencyManager.compilationBegin(this, getModules());
            CompiledDependency compiledDependency = null;
            try {
                compiledDependency = super.compileDependency(dependencyName, dependencyManager);
                compilationTimeLoggingDependencyManager.compilationCompleted(this,
                    !compiledDependency.getCompiledOpenClass().hasErrors());
                return compiledDependency;
            } finally {
                if (compiledDependency == null) {
                    compilationTimeLoggingDependencyManager.compilationCompleted(this, false);
                }
            }
        } else {
            return super.compileDependency(dependencyName, dependencyManager);
        }
    }
}