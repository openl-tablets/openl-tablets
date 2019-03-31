package org.openl.rules.ruleservice.core;

/**
 * Main rule service runtime exception. All exceptions in rule service project should extend from this exception.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -5355895091110317542L;

    /**
     * Constructs a new RuleServiceException.
     */
    public RuleServiceRuntimeException() {
        super();
    }

    /**
     * Constructs a new RuleServiceException with the specified detail message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RuleServiceException with the specified detail message
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public RuleServiceRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceException with a cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RuleServiceRuntimeException(Throwable cause) {
        super(cause);
    }
}
