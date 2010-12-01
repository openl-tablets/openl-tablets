package org.openl.rules.project.instantiation;

import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.project.model.Module;

public class RulesInstantiationStrategyFactory {
    /**
     * @return {@link RulesInstantiationStrategy} instance that will compile
     *         {@link Module}
     */
    public static RulesInstantiationStrategy getStrategy(Module moduleInfo) {
        return getStrategy(moduleInfo, false, null);
    }

    public static RulesInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode, IDependencyManager dependencyManager) {
        return getStrategy(moduleInfo, executionMode, dependencyManager, null);
    }
    
    public static RulesInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        switch (moduleInfo.getType()) {
            case DYNAMIC:
                return new EngineFactoryInstantiationStrategy(moduleInfo, executionMode, dependencyManager, classLoader);
            case STATIC:
                return new WrapperAdjustingInstantiationStrategy(moduleInfo, executionMode, dependencyManager, classLoader);
            case API:
                return new ApiBasedEngineFactoryInstantiationStrategy(moduleInfo, executionMode, dependencyManager, classLoader);
            default:
                throw new OpenLRuntimeException(String.format("Cannot resolve instantiation strategy for \"%s\"",
                        moduleInfo.getType().toString()));
        }
    }
}