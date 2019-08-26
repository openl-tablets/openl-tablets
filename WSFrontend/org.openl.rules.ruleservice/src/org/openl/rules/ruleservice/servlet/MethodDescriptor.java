package org.openl.rules.ruleservice.servlet;

import java.util.List;

public class MethodDescriptor {

    private final String name;
    private final List<String> paramTypes;
    private final String returnType;

    public MethodDescriptor(String name, String returnType, List<String> paramTypes) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public List<String> getParamTypes() {
        return paramTypes;
    }

    public String getReturnType() {
        return returnType;
    }
}
