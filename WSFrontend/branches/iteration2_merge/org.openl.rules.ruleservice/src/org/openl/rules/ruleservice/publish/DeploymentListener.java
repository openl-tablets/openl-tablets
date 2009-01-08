package org.openl.rules.ruleservice.publish;

import java.util.EventListener;
import java.util.Map;

import org.openl.main.OpenLWrapper;

public interface DeploymentListener extends EventListener {

    public void beforeDeployment(String deploymentName);

    public void afterDeployment(String deploymentName, Map<String, OpenLWrapper> ruleModules);
    
    public void beforeUndeployment(String deploymentName);

    public void afterUndeployment(String deploymentName);
}
