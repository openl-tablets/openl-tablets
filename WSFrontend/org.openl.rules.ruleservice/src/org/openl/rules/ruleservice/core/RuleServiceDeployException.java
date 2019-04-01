package org.openl.rules.ruleservice.core;

/**
 * Exception for issues that occurs while service deploying.
 *
 * @author Marat Kamalov
 *
 */
public class RuleServiceDeployException extends RuleServiceException {
    private static final long serialVersionUID = -5393130145512014248L;

    /**
     * Constructs a new RuleServiceDeployException.
     */
    public RuleServiceDeployException() {
        super();
    }

    /**
     * Constructs a new RuleServiceDeployException with the specified detail message
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public RuleServiceDeployException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceDeployException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceDeployException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RuleServiceDeployException with a cause.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceDeployException(Throwable cause) {
        super(cause);
    }
}
