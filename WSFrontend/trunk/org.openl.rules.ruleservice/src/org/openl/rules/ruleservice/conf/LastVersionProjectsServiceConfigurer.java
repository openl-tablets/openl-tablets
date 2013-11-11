package org.openl.rules.ruleservice.conf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

/**
 * Selects the latest deployments and deploys each of their projects as single
 * service.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class LastVersionProjectsServiceConfigurer extends AbstractRulesDeployServiceConfigurer {
    @Override
    protected Collection<Deployment> getDeploymentsFromRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        Map<String, Deployment> latestDeployments = new HashMap<String, Deployment>();
        for (Deployment deployment : ruleServiceLoader.getDeployments()) {
            String deploymentName = deployment.getDeploymentName();
            if (latestDeployments.containsKey(deploymentName)) {
                if (latestDeployments.get(deploymentName).getCommonVersion().compareTo(deployment.getCommonVersion()) < 0) {
                    latestDeployments.put(deploymentName, deployment);
                }
            } else {
                latestDeployments.put(deploymentName, deployment);
            }
        }
        return latestDeployments.values();
    }
}
