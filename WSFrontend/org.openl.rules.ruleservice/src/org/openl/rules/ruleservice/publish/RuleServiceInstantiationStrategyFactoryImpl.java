package org.openl.rules.ruleservice.publish;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.ruleservice.publish.lazy.LazyInstantiationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for RuleServiceInstantiationStrategyFactory. Delegates
 * decision to RulesInstantiationStrategyFactory if one module in service.
 * Returns LazyMultiModuleInstantiationStrategy strategy if more than one module
 * in service.
 * 
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceInstantiationStrategyFactoryImpl implements RuleServiceInstantiationStrategyFactory {

    private boolean lazy = true;
    private Set<String> skippedLazy = Collections.emptySet();
    private final Logger log = LoggerFactory.getLogger(RuleServiceInstantiationStrategyFactoryImpl.class);

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setSkippedLazy(Set<String> skippedLazy) {
        this.skippedLazy = skippedLazy;
    }

    /** {@inheritDoc} */
    public RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription == null) {
            throw new IllegalStateException("ServiceDescription not found!");
        }
        int moduleSize = modules.size();
        if (moduleSize == 0) {
            throw new IllegalStateException("There are no modules to instantiate.");
        }
        String serviceName = serviceDescription.getName();

        if (isLazy() && !skippedLazy.contains(serviceName)) {
            log.info("LAZY loading: [{}]", serviceName);
            return new LazyInstantiationStrategy(serviceDescription.getDeployment(), modules, dependencyManager);
        }
        if (moduleSize == 1) {
            log.info("Single module loading: [{}]", serviceName);
            Module module = modules.iterator().next();
            return RulesInstantiationStrategyFactory.getStrategy(module, true, dependencyManager);
        }
        log.info("Multi module loading: [{}]", serviceName);
        return new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager, true);
    }
}
