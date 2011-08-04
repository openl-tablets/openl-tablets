package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.List;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.InitializingListener;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.cache.LazyMultiModuleInstantiationStrategy;

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
        switch (modules.size()) {
            case 0:
                throw new RuntimeException("There are no modules to instantiate.");
            default:
                LazyMultiModuleInstantiationStrategy myInstantiationStrategy = new LazyMultiModuleInstantiationStrategy(modules, true, dependencyManager);
                if (initializingListeners != null) {
                    for (InitializingListener listener : initializingListeners) {
                        myInstantiationStrategy.addInitializingListener(listener);
                    }
                }
                return myInstantiationStrategy;
        }
    }

}
