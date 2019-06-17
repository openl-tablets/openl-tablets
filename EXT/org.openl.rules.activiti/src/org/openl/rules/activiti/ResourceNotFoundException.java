package org.openl.rules.activiti;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -4082777458792876576L;

    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }
}
