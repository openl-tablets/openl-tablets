package org.openl.rules.ui.repository.beans;

public class CannotFindEntityException extends Exception {
    private static final long serialVersionUID = 7194490449387445459L;

    public CannotFindEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotFindEntityException(String message) {
        super(message);
    }
}
