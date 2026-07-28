package org.openl.rules.openapi.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OperationInfo {
    @Getter
    private final String method;
    @Getter
    private final String produces;
    @Getter
    private final String consumes;

}
