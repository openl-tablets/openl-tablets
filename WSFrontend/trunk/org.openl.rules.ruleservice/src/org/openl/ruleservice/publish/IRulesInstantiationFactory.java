package org.openl.ruleservice.publish;

import java.util.List;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;

public interface IRulesInstantiationFactory {
    RulesInstantiationStrategy getStrategy(List<Module> modules, IDependencyManager dependencyManager);
}
