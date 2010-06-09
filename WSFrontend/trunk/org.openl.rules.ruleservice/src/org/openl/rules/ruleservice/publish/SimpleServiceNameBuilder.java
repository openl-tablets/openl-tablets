package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.loader.DeploymentInfo;

public class SimpleServiceNameBuilder implements ServiceNameBuilder{

    public String getServiceName(DeploymentInfo di) {
        return di.getName();
    }

}
