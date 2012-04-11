package org.openl.rules.ruleservice.client.mapping;

import org.openl.rules.ruleservice.client.OpenLClientException;

public class OpenLClientMappingException extends OpenLClientException{

    private static final long serialVersionUID = 1264034277047907823L;

    public OpenLClientMappingException() {
        super();
    }

    public OpenLClientMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenLClientMappingException(String message) {
        super(message);
    }

    public OpenLClientMappingException(Throwable cause) {
        super(cause);
    }

}
