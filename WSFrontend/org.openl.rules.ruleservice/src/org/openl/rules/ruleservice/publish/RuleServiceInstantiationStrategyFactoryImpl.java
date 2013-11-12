package org.openl.rules.ruleservice.publish;

import java.util.Collection;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.lazy.LazyInstantiationStrategy;

/**
 * Default implementation for RuleServiceInstantiationStrategyFactory. Delegates
 * decision to RulesInstantiationStrategyFactory if one module in service.
 * Returns LazyMultiModuleInstantiationStrategy strategy if more than one module
 * in service.
 * 
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceInstantiationStrategyFactoryImpl implements RuleServiceInstantiationStrategyFactory {

    private boolean lazy = true;

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public boolean isLazy() {
        return lazy;
    }

    /** {@inheritDoc} */
    public RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        switch (modules.size()) {
            case 0:
                throw new IllegalStateException("There are no modules to instantiate.");
            case 1:
                if (isLazy()) {
                    return new LazyInstantiationStrategy(modules, dependencyManager);
                } else {
                    return RulesInstantiationStrategyFactory.getStrategy(modules.iterator().next(),
                        true,
                        dependencyManager);
                }
            default:
                if (isLazy()) {
                    return new LazyInstantiationStrategy(modules, dependencyManager);
                } else {
                    return new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager);
                }
        }
    }

}
