package org.openl.rules.ruleservice.core;

import java.util.Collection;

import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.instantiation.AbstractProjectDependencyManager;
import org.openl.rules.project.instantiation.SimpleProjectDependencyLoader;
import org.openl.rules.project.model.Module;

final class RuleServiceDependencyLoader extends SimpleProjectDependencyLoader {
	RuleServiceDependencyLoader(String dependencyName, Collection<Module> modules, boolean isProject) {
		super(dependencyName, modules, false, true, isProject);
	}

	@Override
	protected CompiledDependency compileDependency(String dependencyName,
			AbstractProjectDependencyManager dependencyManager) throws OpenLCompilationException {
		if (dependencyManager instanceof CompilationTimeLoggingDependencyManager) {
			CompilationTimeLoggingDependencyManager compilationTimeLoggingDependencyManager = (CompilationTimeLoggingDependencyManager) dependencyManager;
			compilationTimeLoggingDependencyManager.compilationBegin(this, getModules());
			CompiledDependency compiledDependency = null;
			try {
				compiledDependency = super.compileDependency(dependencyName, dependencyManager);
				compilationTimeLoggingDependencyManager.compilationCompleted(this, !compiledDependency.getCompiledOpenClass().hasErrors());
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