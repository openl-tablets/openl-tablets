package org.openl.rules.ruleservice.core;

/**
 * Exception for wrapping exceptions for returning them from ruleservice.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceWrapperException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = 3618613334261575918L;

    /**
     * Constructs a new RuleServiceWrapperException.
     */
    public RuleServiceWrapperException() {
        super();
    }

    /**
     * Constructs a new RuleServiceWrapperException with the specified detail
     * message
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     */
    public RuleServiceWrapperException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceWrapperException with the specified detail
     * message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public RuleServiceWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RuleServiceWrapperException with a cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public RuleServiceWrapperException(Throwable cause) {
        super(cause);
    }
}
