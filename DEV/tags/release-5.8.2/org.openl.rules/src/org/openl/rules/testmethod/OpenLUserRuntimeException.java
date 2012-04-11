package org.openl.rules.testmethod;

import org.openl.exception.OpenLRuntimeException;

public class OpenLUserRuntimeException extends OpenLRuntimeException {

    private static final long serialVersionUID = -6327856390127472928L;

    public OpenLUserRuntimeException(String message) {
        super(message);
    }

    public OpenLUserRuntimeException(Throwable cause) {
        super(cause);
    }

}
