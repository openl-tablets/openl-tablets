package org.openl.rules.ruleservice.core;

public class ExceptionDetails {

    private final String code;
    private final String message;
    private final Object[] args;

    /**
     * Initialize rule service exception details
     *
     * @param code error message code
     * @param message the message of error
     * @param args error message argument
     */
    public ExceptionDetails(String code, String message, Object[] args) {
        this.code = code;
        this.message = message;
        this.args = args;
    }

    /**
     * Initialize rule service exception details
     *
     * @param message the message of error
     */
    public ExceptionDetails(String message) {
        this(null, message, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }
}
