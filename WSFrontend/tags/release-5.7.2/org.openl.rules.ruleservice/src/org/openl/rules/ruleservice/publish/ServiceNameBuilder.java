package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.loader.DeploymentInfo;

public interface ServiceNameBuilder {
    /**
     * Gets service name for deployment.
     * 
     * @param di deployment
     * @return Service name for specified deployment.
     */
    String getServiceName(DeploymentInfo di);
}
