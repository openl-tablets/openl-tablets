package org.openl.rules.ruleservice.core;

import java.util.Objects;

/**
 * Exception for wrapping exceptions for returning them from ruleservice.
 *
 * @author Marat Kamalov
 */
public class RuleServiceWrapperException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = 3618613334261575918L;

    private final ExceptionDetails details;
    private final ExceptionType type;

    /**
     * Constructs a new RuleServiceWrapperException with the specified detail message and cause.
     *
     * @param details error details
     * @param type    the message type
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *                value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceWrapperException(ExceptionDetails details, ExceptionType type, String message, Throwable cause) {
        super(message, cause);
        this.details = Objects.requireNonNull(details, "Details is required!");
        this.type = type;
    }

    /**
     * Returns simple Message
     */
    public ExceptionDetails getDetails() {
        return details;
    }

    /**
     * Returns error type
     */
    public ExceptionType getType() {
        return type;
    }
}
