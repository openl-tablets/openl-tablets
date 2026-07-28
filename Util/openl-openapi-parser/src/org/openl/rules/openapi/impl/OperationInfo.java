package org.openl.rules.openapi.impl;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OperationInfo {
    private final String method;
    private final String produces;
    private final String consumes;

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
