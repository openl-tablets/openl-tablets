package org.openl.rules.ruleservice.core;

import java.util.Collection;

import org.openl.rules.project.instantiation.SimpleProjectDependencyLoader;
import org.openl.rules.project.model.Module;

final class RuleServiceDependencyLoader extends SimpleProjectDependencyLoader {
    RuleServiceDependencyLoader(String dependencyName, Collection<Module> modules) {
        super(dependencyName, modules, false, true);
    }
}