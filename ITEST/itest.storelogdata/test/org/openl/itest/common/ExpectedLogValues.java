package org.openl.itest.common;

public class ExpectedLogValues {
    public String request;
    public String response;
    public String methodName;
    public String serviceName;
    public String publisherType;

    public ExpectedLogValues(String request,
                             String response,
                             String methodName,
                             String serviceName,
                             String publisherType) {
        this.request = request;
        this.response = response;
        this.methodName = methodName;
        this.serviceName = serviceName;
        this.publisherType = publisherType;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getPublisherType() {
        return publisherType;
    }

    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public boolean isResponseProvided() {
        return response != null;
    }
}
