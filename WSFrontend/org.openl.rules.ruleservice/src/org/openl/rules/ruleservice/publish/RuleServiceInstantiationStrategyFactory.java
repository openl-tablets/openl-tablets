package org.openl.rules.ruleservice.publish;

import java.util.Collection;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
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
     * @param modules modules
     * @param dependencyManager
     * @return 
     */
    RulesInstantiationStrategy getStrategy(ServiceDescription serviceDescription, IDependencyManager dependencyManager);
}
