package org.openl.rules.ruleservice.publish;

import java.util.Collection;
import java.util.Objects;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for RuleServiceInstantiationStrategyFactory. Delegates decision to
 * RulesInstantiationStrategyFactory if one module in service.
 *
 *
 * @author Marat Kamalov
 *
 */
public class RuleServiceInstantiationStrategyFactoryImpl implements RuleServiceInstantiationStrategyFactory {

    private final Logger log = LoggerFactory.getLogger(RuleServiceInstantiationStrategyFactoryImpl.class);

    /** {@inheritDoc} */
    @Override
    public RulesInstantiationStrategy getStrategy(ServiceDescription serviceDescription,
            IDependencyManager dependencyManager) {
        Objects.requireNonNull(serviceDescription, "serviceDescription cannot be null");
        Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        Collection<Module> modules = serviceDescription.getModules();
        int moduleSize = modules.size();
        if (moduleSize == 0) {
            throw new IllegalStateException("There are no modules to instantiate.");
        }

        log.debug("Multi module loading strategy has been used for service '{}'.", serviceDescription.getDeployPath());
        return new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager, true);
    }
}
