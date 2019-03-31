package org.openl.rules.ruleservice.core;

/**
 * Exception for wrapping exceptions for returning them from ruleservice.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceWrapperException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = 3618613334261575918L;

    private String simpleMessage;

    private ExceptionType type;

    /**
     * Constructs a new RuleServiceWrapperException with the specified detail message and cause.
     * 
     * @param simpleMessage the message of error
     * @param type the message type
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceWrapperException(String simpleMessage, ExceptionType type, String message, Throwable cause) {
        super(message, cause);
        this.simpleMessage = simpleMessage;
        this.type = type;
    }

    /**
     * Returns simple Message
     */
    public String getSimpleMessage() {
        return simpleMessage;
    }

    /**
     * Returns error type
     */
    public ExceptionType getType() {
        return type;
    }
}
