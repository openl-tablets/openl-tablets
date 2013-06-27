package org.openl.rules.ruleservice.publish;

import java.util.Collection;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.cache.LazyMultiModuleInstantiationStrategy;

/**
 * Default implementation for RuleServiceInstantiationStrategyFactory. Delegates
 * decision to RulesInstantiationStrategyFactory if one module in service.
 * Returns LazyMultiModuleInstantiationStrategy strategy if more than ome module
 * in service.
 * 
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceInstantiationStrategyFactoryImpl implements RuleServiceInstantiationStrategyFactory {
	
	/** {@inheritDoc} */
    public RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        switch (modules.size()) {
            case 0:
                throw new IllegalStateException("There are no modules to instantiate.");
            case 1:
                return RulesInstantiationStrategyFactory
                        .getStrategy(modules.iterator().next(), true, dependencyManager);
            default:
                return new LazyMultiModuleInstantiationStrategy(modules, dependencyManager);
        }
    }

}
