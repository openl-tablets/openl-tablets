package org.openl.ruleservice.publish;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;

public interface IRulesInstantiationFactory {
    RulesInstantiationStrategy getStrategy(Module moduleInfo,IDependencyManager dependencyManager);
}
