package org.openl.rules.ruleservice.publish;

import java.util.List;

import org.openl.rules.ruleservice.resolver.RuleServiceInfo;

public interface DeploymentAdmin {
    void deploy(String deploymentName, ClassLoader loader, List<RuleServiceInfo> infoList);

    void undeploy(String deploymentName);
}
