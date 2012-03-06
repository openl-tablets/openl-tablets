package org.openl.rules.ruleservice.multimodule;

import java.util.List;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactory;
import org.openl.rules.ruleservice.publish.cache.dispatcher.DispatchedMultiModuleInstantiationStrategy;

public class TestInstantiationFactory implements RuleServiceInstantiationStrategyFactory {

    @Override
    public RulesInstantiationStrategy getStrategy(List<Module> modules, IDependencyManager dependencyManager) {
        if (modules.size() == 0) {
            throw new RuntimeException("There are no modules to instantiate.");

        } else {
            return new DispatchedMultiModuleInstantiationStrategy(modules, true, dependencyManager);
        }
    }

}
