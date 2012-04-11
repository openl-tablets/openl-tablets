package org.openl.rules.ruleservice.client;

public class OpenLClientException extends Exception{

    private static final long serialVersionUID = 1L;

    public OpenLClientException() {
        super();
    }

    public OpenLClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenLClientException(String message) {
        super(message);
    }

    public OpenLClientException(Throwable cause) {
        super(cause);
    }
    
}
