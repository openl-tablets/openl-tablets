package org.openl.rules.ruleservice.instantiation;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.ruleservice.resolver.RuleServiceInfo;

public class RulesInstantiationStrategyFactory {
    private static final Log LOG = LogFactory.getLog(RulesInstantiationStrategyFactory.class);

    /**
     * Gets <code>RulesInstantiationStrategy</code> by information about rules project and 
     * requires class loader that rules will be working with.
     * 
     * @param wsInfo information about rules project
     * @param classLoader class loader that rules will be working with
     * @return <code>RulesInstantiationStrategy</code> instance
     */
    public static RulesInstantiationStrategy getStrategy(RuleServiceInfo wsInfo, ClassLoader classLoader) {
        String className = wsInfo.getClassName();

        switch (wsInfo.getServiceType()) {
            case DYNAMIC_WRAPPER:
                return new EngineFactoryInstantiationStrategy(wsInfo.getXlsFile(), className, classLoader);
            case STATIC_WRAPPER:
                String path = ".";
                try {
                    path = wsInfo.getProject().getCanonicalPath();
                } catch (IOException e) {
                    LOG.error("Failed to get canonical path of rules project location", e);
                }
                return new WrapperAdjustingInstantiationStrategy(path, className, classLoader);
            case AUTO_WRAPPER:
                return new WebServiceEngineFactoryInstantiationStrategy(wsInfo.getXlsFile(), className, classLoader);
            default:
                throw new OpenLRuntimeException(String.format("Cannot resolve instantiation strategy for \"%s\"", wsInfo
                        .getServiceType().toString()));
        }
    }
}