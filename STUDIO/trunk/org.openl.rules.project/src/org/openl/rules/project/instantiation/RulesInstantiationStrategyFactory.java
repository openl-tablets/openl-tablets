package org.openl.rules.project.instantiation;

import org.openl.rules.project.model.Module;

public class RulesInstantiationStrategyFactory {
    /**
     * @return {@link RulesInstantiationStrategy} instance that will compile
     *         {@link Module}
     */
    public static RulesInstantiationStrategy getStrategy(Module moduleInfo) {
        switch (moduleInfo.getType()) {
            // case DYNAMIC:
            case STATIC:
                return new WrapperAdjustingInstantiationStrategy(moduleInfo);
                // case API:
        }
        return null;
    }
}