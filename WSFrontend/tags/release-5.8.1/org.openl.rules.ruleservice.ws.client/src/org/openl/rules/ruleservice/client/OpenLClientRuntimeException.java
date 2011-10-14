package org.openl.rules.ruleservice.client;

public class OpenLClientRuntimeException extends RuntimeException{

    private static final long serialVersionUID = 7118642272939622660L;

    public OpenLClientRuntimeException() {
        super();
    }

    public OpenLClientRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenLClientRuntimeException(String message) {
        super(message);
    }

    public OpenLClientRuntimeException(Throwable cause) {
        super(cause);
    }
    
}
