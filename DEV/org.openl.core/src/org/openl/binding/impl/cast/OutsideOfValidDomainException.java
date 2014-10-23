package org.openl.binding.impl.cast;

public class OutsideOfValidDomainException extends RuntimeException {

    private static final long serialVersionUID = 8536909104810936082L;

    public OutsideOfValidDomainException() {
        super();
    }

    public OutsideOfValidDomainException(String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public OutsideOfValidDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutsideOfValidDomainException(String message) {
        super(message);
    }

    public OutsideOfValidDomainException(Throwable cause) {
        super(cause);
    }

}
