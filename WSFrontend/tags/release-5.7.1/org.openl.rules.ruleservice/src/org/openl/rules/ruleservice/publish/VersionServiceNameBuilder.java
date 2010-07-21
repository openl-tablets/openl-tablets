package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.loader.DeploymentInfo;

public class VersionServiceNameBuilder implements ServiceNameBuilder {

    public String getServiceName(DeploymentInfo di) {
        return di.getName() + di.getVersion().getVersionName().replace('.', '-');
    }

}
