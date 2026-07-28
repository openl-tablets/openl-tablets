package org.openl.rules.ruleservice.deployer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum DeploymentDescriptor {

    XML("deployment.xml"),
    YAML("deployment.yaml");

    @Getter
    private final String fileName;
}
