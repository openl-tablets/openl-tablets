package org.openl.rules.ruleservice.core;

/**
 * Exception for issues that occurs while service undeploying.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceUndeployException extends RuleServiceException {
    private static final long serialVersionUID = -5393130145512014248L;

    /**
     * Constructs a new RuleServiceUndeployException.
     */
    public RuleServiceUndeployException() {
        super();
    }

    /**
     * Constructs a new RuleServiceUndeployException with the specified detail message
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public RuleServiceUndeployException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceUndeployException with the specified detail message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceUndeployException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RuleServiceUndeployException with a cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceUndeployException(Throwable cause) {
        super(cause);
    }
}
