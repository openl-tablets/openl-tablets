package org.openl.rules.ruleservice.core;

/**
 * Main exception for rule service.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceException extends Exception {

    private static final long serialVersionUID = -5355895091110317542L;

    /**
     * Constructs a new RuleServiceSystemException.
     */
    public RuleServiceException() {
        super();
    }

    /**
     * Constructs a new RuleServiceException with the specified detail message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RuleServiceException with the specified detail message
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public RuleServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceException with a cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceException(Throwable cause) {
        super(cause);
    }
}