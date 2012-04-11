package org.openl.rules.ruleservice.publish;

import java.util.List;

import org.openl.rules.ruleservice.resolver.RulesProjectInfo;

public interface DeploymentAdmin {
    void deploy(String deploymentName, ClassLoader loader, List<RulesProjectInfo> infoList);

    void undeploy(String deploymentName);
}
