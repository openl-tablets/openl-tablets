package org.openl.rules.activiti;

public class MultipleMethodsFoundException extends RuntimeException {

    private static final long serialVersionUID = -2134177718752351064L;

    public MultipleMethodsFoundException() {
        super();
    }

    public MultipleMethodsFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipleMethodsFoundException(String message) {
        super(message);
    }

    public MultipleMethodsFoundException(Throwable cause) {
        super(cause);
    }

}
