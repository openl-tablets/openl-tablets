package org.openl.rules.activiti;

public class MethodNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 9018051982378241633L;

    public MethodNotFoundException() {
        super();
    }

    public MethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodNotFoundException(String message) {
        super(message);
    }

    public MethodNotFoundException(Throwable cause) {
        super(cause);
    }

}
