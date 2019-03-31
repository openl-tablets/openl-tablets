package org.openl.rules.ruleservice.core;

import java.util.Collection;

import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.model.Module;

public interface CompilationTimeLoggingDependencyManager {

    void compilationBegin(IDependencyLoader dependencyLoader, Collection<Module> modules);

    void compilationCompleted(IDependencyLoader dependencyLoader, boolean successed);
}
