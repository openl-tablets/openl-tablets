package org.openl.rules.ruleservice.conf;

import java.util.Collection;

import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

/**
 * Selects all versions of deployments and deploys each of their projects as
 * single service.
 */
public class AllDeploymentsServiceConfigurer extends AbstractRulesDeployServiceConfigurer {

    @Override
    protected Collection<Deployment> getDeploymentsFromRuleServiceLoader(RuleServiceLoader loader) {
        return loader.getDeployments();
    }

}
