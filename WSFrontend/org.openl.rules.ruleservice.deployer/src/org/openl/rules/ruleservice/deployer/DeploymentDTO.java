package org.openl.rules.ruleservice.deployer;

import java.io.InputStream;

class DeploymentDTO {

    private final String name;
    private final InputStream inputStream;
    private final Integer contentSize;

    public DeploymentDTO(String name, InputStream inputStream, Integer contentSize) {
        this.name = name;
        this.inputStream = inputStream;
        this.contentSize = contentSize;
    }

    public String getName() {
        return name;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Integer getContentSize() {
        return contentSize;
    }
}
