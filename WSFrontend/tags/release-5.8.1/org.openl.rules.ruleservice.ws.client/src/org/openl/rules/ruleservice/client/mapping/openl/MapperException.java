package org.openl.rules.ruleservice.client.mapping.openl;

import org.openl.rules.ruleservice.client.OpenLClientException;

public class MapperException extends OpenLClientException{

    private static final long serialVersionUID = -5497250511970166116L;

    public MapperException() {
        super();
    }

    public MapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public MapperException(String message) {
        super(message);
    }

    public MapperException(Throwable cause) {
        super(cause);
    }
}
