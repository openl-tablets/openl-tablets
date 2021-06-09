package org.openl.rules.ruleservice.deployer;

public enum DeploymentDescriptor {

    XML("deployment.xml"),
    YAML("deployment.yaml");

    private final String fileName;

    DeploymentDescriptor(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
