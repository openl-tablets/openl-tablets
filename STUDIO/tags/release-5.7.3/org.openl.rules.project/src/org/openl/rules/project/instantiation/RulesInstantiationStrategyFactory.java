package org.openl.rules.project.instantiation;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.project.model.Module;

public class RulesInstantiationStrategyFactory {
    /**
     * @return {@link RulesInstantiationStrategy} instance that will compile
     *         {@link Module}
     */
    public static RulesInstantiationStrategy getStrategy(Module moduleInfo) {
        return getStrategy(moduleInfo, false);
    }

    public static RulesInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode) {
        switch (moduleInfo.getType()) {
            case DYNAMIC:
                return new EngineFactoryInstantiationStrategy(moduleInfo, executionMode);
            case STATIC:
                return new WrapperAdjustingInstantiationStrategy(moduleInfo, executionMode);
            case API:
                return new ApiBasedEngineFactoryInstantiationStrategy(moduleInfo, executionMode);
            default:
                throw new OpenLRuntimeException(String.format("Cannot resolve instantiation strategy for \"%s\"",
                        moduleInfo.getType().toString()));
        }
    }
}