package org.openl.rules.openapi.impl;

public class OperationInfo {
    private String method;
    private String produces;
    private String consumes;

    public OperationInfo(String method, String produces, String consumes) {
        this.method = method;
        this.produces = produces;
        this.consumes = consumes;
    }

    public String getMethod() {
        return method;
    }

    public String getProduces() {
        return produces;
    }

    public String getConsumes() {
        return consumes;
    }

}
