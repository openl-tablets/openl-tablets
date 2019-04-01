package org.openl.rules.ruleservice.publish;

import java.util.Collection;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.publish.lazy.LazyInstantiationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for RuleServiceInstantiationStrategyFactory. Delegates decision to
 * RulesInstantiationStrategyFactory if one module in service. Returns LazyMultiModuleInstantiationStrategy strategy if
 * more than one module in service.
 *
 *
 * @author Marat Kamalov
 *
 */
public class RuleServiceInstantiationStrategyFactoryImpl implements RuleServiceInstantiationStrategyFactory {

    private boolean lazy = true;
    private final Logger log = LoggerFactory.getLogger(RuleServiceInstantiationStrategyFactoryImpl.class);

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public boolean isLazy() {
        return lazy;
    }

    /** {@inheritDoc} */
    @Override
    public RulesInstantiationStrategy getStrategy(ServiceDescription serviceDescription,
            IDependencyManager dependencyManager) {
        Collection<Module> modules = serviceDescription.getModules();
        int moduleSize = modules.size();
        if (moduleSize == 0) {
            throw new IllegalStateException("There are no modules to instantiate.");
        }
        String serviceName = serviceDescription.getName();

        if (isLazy()) {
            log.debug("Lazy loading strategy has been used for service: '{}'.", serviceName);
            return new LazyInstantiationStrategy(serviceDescription.getDeployment(), modules, dependencyManager);
        }
        if (moduleSize == 1) {
            log.debug("Single module loading strategy has been used for service: '{}'.", serviceName);
            Module module = modules.iterator().next();
            return RulesInstantiationStrategyFactory.getStrategy(module, true, dependencyManager);
        }
        log.debug("Multi module loading strategy has been used for service: '{}'.", serviceName);
        return new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager, true);
    }
}
