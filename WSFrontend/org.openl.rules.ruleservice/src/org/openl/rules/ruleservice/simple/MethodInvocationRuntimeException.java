package org.openl.rules.ruleservice.simple;

public class MethodInvocationRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MethodInvocationRuntimeException() {
        super();
    }

    public MethodInvocationRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodInvocationRuntimeException(String message) {
        super(message);
    }

    public MethodInvocationRuntimeException(Throwable cause) {
        super(cause);
    }

}
