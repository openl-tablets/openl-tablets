package org.openl.binding.impl.cast;

import org.openl.exception.OpenLRuntimeException;

public class OutsideOfValidDomainException extends OpenLRuntimeException {

    private static final long serialVersionUID = 8536909104810936082L;

    public OutsideOfValidDomainException() {
        super();
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
