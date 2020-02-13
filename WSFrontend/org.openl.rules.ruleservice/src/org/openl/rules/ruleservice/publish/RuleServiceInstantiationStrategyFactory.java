package org.openl.rules.ruleservice.publish;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.ruleservice.core.ServiceDescription;

/**
 * Resolve strategy for creating service bean.
 *
 * @author Marat Kamalov
 *
 */
public interface RuleServiceInstantiationStrategyFactory {
    /**
     * Returns strategy for creating service bean.
     *
     * @param serviceDescription serviceDescription
     * @param dependencyManager
     * @return
     */
    RulesInstantiationStrategy getStrategy(ServiceDescription serviceDescription, IDependencyManager dependencyManager);
}
