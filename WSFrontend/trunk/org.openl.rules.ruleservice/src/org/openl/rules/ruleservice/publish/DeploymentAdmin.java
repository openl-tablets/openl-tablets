package org.openl.rules.ruleservice.publish;

import java.util.List;

import org.openl.rules.project.model.ProjectDescriptor;

public interface DeploymentAdmin {
    void deploy(String deploymentName, List<ProjectDescriptor> infoList);

    void undeploy(String deploymentName);
}
