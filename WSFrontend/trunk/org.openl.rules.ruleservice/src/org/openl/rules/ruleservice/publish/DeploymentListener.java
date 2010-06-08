package org.openl.rules.ruleservice.publish;

import java.util.EventListener;
import java.util.Map;

import org.openl.main.OpenLWrapper;

public interface DeploymentListener extends EventListener {

    void afterDeployment(String deploymentName, Map<String, OpenLWrapper> ruleModules);

    void afterUndeployment(String deploymentName);

    void beforeDeployment(String deploymentName);

    void beforeUndeployment(String deploymentName);
}
