package org.openl.rules.ruleservice.storelogdata.cassandra.annotation;

public class DaoCreationException extends Exception {

    private static final long serialVersionUID = 1L;

    public DaoCreationException() {
        super();
    }

    public DaoCreationException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DaoCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoCreationException(String message) {
        super(message);
    }

    public DaoCreationException(Throwable cause) {
        super(cause);
    }

}
