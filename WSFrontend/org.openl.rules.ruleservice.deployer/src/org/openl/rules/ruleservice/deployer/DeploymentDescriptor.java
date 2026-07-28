package org.openl.rules.ruleservice.deployer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum DeploymentDescriptor {

    XML("deployment.xml"),
    YAML("deployment.yaml");

    private final String fileName;

    public String getFileName() {
        return fileName;
    }
}
