package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.InitializingListener;
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

    private Collection<InitializingListener> initializingListeners;

    public Collection<InitializingListener> getInitializingListeners() {
        return initializingListeners;
    }

    public void setInitializingListeners(Collection<InitializingListener> initializingListeners) {
        this.initializingListeners = initializingListeners;
    }

    public void addInitializingListener(InitializingListener listener) {
        if (initializingListeners == null) {
            initializingListeners = new ArrayList<InitializingListener>();
        }
        initializingListeners.add(listener);
    }

    public void removeInitializingListener(InitializingListener listener) {
        if (initializingListeners != null) {
            initializingListeners.remove(listener);
        }
    }
    
    /** {@inheritDoc} */
    public RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        switch (modules.size()) {
            case 0:
                throw new RuntimeException("There are no modules to instantiate.");
            case 1:
                return RulesInstantiationStrategyFactory.getStrategy(modules.iterator().next(), true, dependencyManager);
            default:
                LazyMultiModuleInstantiationStrategy myInstantiationStrategy = new LazyMultiModuleInstantiationStrategy(
                        modules, true, dependencyManager);
                if (initializingListeners != null) {
                    for (InitializingListener listener : initializingListeners) {
                        myInstantiationStrategy.addInitializingListener(listener);
                    }
                }
                return myInstantiationStrategy;
        }
    }

}
