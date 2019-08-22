package org.openl.rules.ruleservice.servlet;

import java.util.List;

public class ServiceMethodsInfo {

    private final String methodName;
    private final List<String> methodParams;
    private final String resultType;

    public ServiceMethodsInfo(String methodName, String resultType, List<String> methodParams) {
        this.methodName = methodName;
        this.methodParams = methodParams;
        this.resultType = resultType;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getMethodParams() {
        return methodParams;
    }

    public String getResultType() {
        return resultType;
    }
}
