package org.openl.rules.ruleservice.core;

public class ExceptionDetails {

    private final String code;
    private final String message;
    private final Object body;

    /**
     * Initialize rule service exception details
     *
     * @param code error message code
     * @param message the message of error
     */
    public ExceptionDetails(String code, String message) {
        this.code = code;
        this.message = message;
        this.body = null;
    }

    public ExceptionDetails(Object body) {
        this.body = body;
        this.code = null;
        this.message = null;
    }

    /**
     * Initialize rule service exception details
     *
     * @param message the message of error
     */
    public ExceptionDetails(String message) {
        this(null, message);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getBody() {
        return body;
    }
}
