package org.openl.rules.ruleservice.core;

public class RuleServiceSystemException extends Exception{

    private static final long serialVersionUID = -5355895091110317542L;

    /**
     * Constructs a new RuleServiceSystemException
     */
    public RuleServiceSystemException() {
        super();
    }

    /**
     * Constructs a new RuleServiceSystemException with the specified detail message
     * and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public RuleServiceSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RuleServiceSystemException with the specified detail message
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     */
    public RuleServiceSystemException(String message) {
        super(message);
    }

    /**
     * Constructs a new RuleServiceSystemException with a cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public RuleServiceSystemException(Throwable cause) {
        super(cause);
    }
}