package org.openl.rules.ruleservice.core;

public class ExceptionDetails {

    private final String code;
    private final String message;

    /**
     * Initialize rule service exception details
     *
     * @param code error message code
     * @param message the message of error
     */
    public ExceptionDetails(String code, String message) {
        this.code = code;
        this.message = message;
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

}
