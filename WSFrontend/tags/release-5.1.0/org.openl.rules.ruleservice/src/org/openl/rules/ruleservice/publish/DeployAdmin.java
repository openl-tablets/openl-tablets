package org.openl.rules.ruleservice.publish;

import java.util.List;

public interface DeployAdmin {
    void deploy(String serviceName, ClassLoader loader, List<WSInfo> infoList);
    void undeploy(String serviceName);
}
