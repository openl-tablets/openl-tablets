package org.openl.rules.ruleservice.publish;

import java.util.List;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;

public class RulesInstantiationFactory implements IRulesInstantiationFactory {

    public RulesInstantiationStrategy getStrategy(List<Module> modules, IDependencyManager dependencyManager) {
        if (modules == null) {
            throw new IllegalArgumentException("modules argument can't be null");
        }
        
        switch (modules.size()) {
            case 0:
                throw new RuntimeException("There are no modules to instantiate.");
            case 1:
                return RulesInstantiationStrategyFactory.getStrategy(modules.get(0), true, dependencyManager);
            default:
                return new MultiModuleInstantiationStrategy(modules, true, dependencyManager);
        }
    }

}
