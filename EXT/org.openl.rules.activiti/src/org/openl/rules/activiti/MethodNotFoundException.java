package org.openl.rules.activiti;

public class MethodNotFoundException extends RuntimeException {

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
