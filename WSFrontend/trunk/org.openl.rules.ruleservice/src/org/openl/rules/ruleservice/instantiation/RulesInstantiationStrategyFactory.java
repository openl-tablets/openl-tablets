package org.openl.rules.ruleservice.instantiation;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.ruleservice.resolver.RulesModuleInfo;
import org.openl.rules.ruleservice.resolver.RulesProjectInfo;

public class RulesInstantiationStrategyFactory {
    private static final Log LOG = LogFactory.getLog(RulesInstantiationStrategyFactory.class);

    /**
     * Gets <code>RulesInstantiationStrategy</code> by information about rules project and 
     * requires class loader that rules will be working with.
     * 
     * @param rulesModule information about rules module
     * @param classLoader class loader that rules will be working with
     * @return <code>RulesInstantiationStrategy</code> instance
     */
    public static RulesInstantiationStrategy getStrategy(RulesModuleInfo rulesModule, ClassLoader classLoader) {
        String className = rulesModule.getClassName();

        switch (rulesModule.getServiceType()) {
            case DYNAMIC_WRAPPER:
                return new EngineFactoryInstantiationStrategy(rulesModule.getXlsFile(), className, classLoader);
            case STATIC_WRAPPER:
                String path = ".";
                try {
                    path = rulesModule.getModuleFolder().getCanonicalPath();
                } catch (IOException e) {
                    LOG.error("Failed to get canonical path of rules project location", e);
                }
                return new WrapperAdjustingInstantiationStrategy(path, className, classLoader);
            case AUTO_WRAPPER:
                return new WebServiceEngineFactoryInstantiationStrategy(rulesModule.getXlsFile(), className, classLoader);
            default:
                throw new OpenLRuntimeException(String.format("Cannot resolve instantiation strategy for \"%s\"", rulesModule
                        .getServiceType().toString()));
        }
    }
}