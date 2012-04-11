package org.openl.rules.service;

public class TableServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public TableServiceException() {
    }

    public TableServiceException(String message) {
        super(message);
    }

    public TableServiceException(Throwable cause) {
        super(cause);
    }

    public TableServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
