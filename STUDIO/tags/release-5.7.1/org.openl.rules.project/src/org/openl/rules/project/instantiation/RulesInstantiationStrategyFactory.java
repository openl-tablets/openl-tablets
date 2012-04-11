package org.openl.rules.project.instantiation;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.project.model.Module;

public class RulesInstantiationStrategyFactory {
    /**
     * @return {@link RulesInstantiationStrategy} instance that will compile
     *         {@link Module}
     */
    public static RulesInstantiationStrategy getStrategy(Module moduleInfo) {
        switch (moduleInfo.getType()) {
            case DYNAMIC:
                return new EngineFactoryInstantiationStrategy(moduleInfo);
            case STATIC:
                return new WrapperAdjustingInstantiationStrategy(moduleInfo);
            case API:
                return new ApiBasedEngineFactoryInstantiationStrategy(moduleInfo);
            default:
                throw new OpenLRuntimeException(String.format("Cannot resolve instantiation strategy for \"%s\"",
                        moduleInfo.getType().toString()));
        }
    }
}