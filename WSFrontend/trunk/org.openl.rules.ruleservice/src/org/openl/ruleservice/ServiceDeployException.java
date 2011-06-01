package org.openl.ruleservice;

public class ServiceDeployException extends Exception {
    private static final long serialVersionUID = -5393130145512014248L;

    public ServiceDeployException() {
        super();
    }

    public ServiceDeployException(String message) {
        super(message);
    }

    public ServiceDeployException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceDeployException(Throwable cause) {
        super(cause);
    }
}
