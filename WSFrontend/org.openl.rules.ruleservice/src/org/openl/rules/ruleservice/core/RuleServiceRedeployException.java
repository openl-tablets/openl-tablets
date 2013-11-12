package org.openl.rules.ruleservice.core;

/**
 * Exception for issues that occurs while service redeploying.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceRedeployException extends RuleServiceException {
    private static final long serialVersionUID = -5393130145512014248L;

    /**
     * Constructs a new RuleServiceRedeployException.
     */
    public RuleServiceRedeployException() {
        super();
    }

    /**
     * Constructs a new RuleServiceRedeployException with the specified detail
     * message
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     */
    public RuleServiceRedeployException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceRedeployException with the specified detail
     * message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public RuleServiceRedeployException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RuleServiceRedeployException with a cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public RuleServiceRedeployException(Throwable cause) {
        super(cause);
    }
}
