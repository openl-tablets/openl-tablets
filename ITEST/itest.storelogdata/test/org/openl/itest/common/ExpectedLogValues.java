package org.openl.itest.common;

public class ExpectedLogValues {
    private String request;
    private String response;
    private String methodName;
    private String serviceName;
    private String publisherType;
    private boolean responseProvided;

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
        this.responseProvided = response != null;
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
        return responseProvided;
    }

    public void setResponseProvided(boolean responseProvided) {
        this.responseProvided = responseProvided;
    }
}
