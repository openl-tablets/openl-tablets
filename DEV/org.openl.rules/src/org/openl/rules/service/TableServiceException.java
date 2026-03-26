package org.openl.rules.service;

import java.io.Serial;

public class TableServiceException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public TableServiceException(String message) {
        super(message);
    }

    public TableServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
