package org.openl.rules.project.instantiation;

import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.project.model.Module;

public final class RulesInstantiationStrategyFactory {
    private RulesInstantiationStrategyFactory() {
    }

    /**
     * @return {@link SingleModuleInstantiationStrategy} instance that will compile
     *         {@link Module}
     */
    public static SingleModuleInstantiationStrategy getStrategy(Module moduleInfo) {
        return getStrategy(moduleInfo, false, null);
    }
    
    public static SingleModuleInstantiationStrategy getStrategy(Module moduleInfo, IDependencyManager dependencyManager) {
        return getStrategy(moduleInfo, false, dependencyManager);
    }    

    public static SingleModuleInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode, IDependencyManager dependencyManager) {
        return getStrategy(moduleInfo, executionMode, dependencyManager, null);
    }
    
    public static SingleModuleInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        switch (moduleInfo.getType()) {
            case WRAPPER:
                return new WrapperAdjustingInstantiationStrategy(moduleInfo, executionMode, dependencyManager, classLoader);
            case API:
                return new ApiBasedInstantiationStrategy(moduleInfo, executionMode, dependencyManager, classLoader);
            default:
                throw new OpenLRuntimeException(String.format("Cannot resolve instantiation strategy for \"%s\"",
                        moduleInfo.getType().toString()));
        }
    }
}