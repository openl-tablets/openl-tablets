package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.List;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.InitializingListener;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;

public class RulesInstantiationFactory implements IRulesInstantiationFactory {

    private List<InitializingListener> initializingListeners;

    public List<InitializingListener> getInitializingListeners() {
        return initializingListeners;
    }

    public void setInitializingListeners(List<InitializingListener> initializingListeners) {
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

    public RulesInstantiationStrategy getStrategy(List<Module> modules, IDependencyManager dependencyManager) {
        if (modules == null) {
            throw new IllegalArgumentException("modules argument can't be null");
        }

        switch (modules.size()) {
            case 0:
                throw new IllegalArgumentException("There are no modules to instantiate.");
            case 1:
                return RulesInstantiationStrategyFactory.getStrategy(modules.get(0), true, dependencyManager);
            default:
                MultiModuleInstantiationStrategy multiModuleInstantiationStrategy = new MultiModuleInstantiationStrategy(
                        modules, true, dependencyManager);
                for (InitializingListener listener : initializingListeners) {
                    multiModuleInstantiationStrategy.addInitializingListener(listener);
                }
                return multiModuleInstantiationStrategy;
        }
    }

}
