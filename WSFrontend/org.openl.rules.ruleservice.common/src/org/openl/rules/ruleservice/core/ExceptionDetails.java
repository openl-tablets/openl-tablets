package org.openl.rules.ruleservice.core;

public class ExceptionDetails {

    private final String message;
    private final Object body;

    /**
     * Initialize rule service exception details
     *
     * @param message the message of error
     */
    public ExceptionDetails(String message) {
        this.message = message;
        this.body = null;
    }

    public ExceptionDetails(Object body) {
        this.body = body;
        this.message = null;
    }

    public String getMessage() {
        return message;
    }

    public Object getBody() {
        return body;
    }
}
