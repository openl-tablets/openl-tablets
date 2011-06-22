package org.openl.rules.ruleservice.loader;

import org.openl.rules.ruleservice.core.RuleServiceException;

/**
 * Main data source exception for wrapping data source exceptions
 * 
 * @author MKamalov
 * 
 */
public class DataSourceException extends RuleServiceException {

    private static final long serialVersionUID = 6818824565990021295L;

    /**
     * Constructs a new DataSourceException
     */
    public DataSourceException() {
        super();
    }

    /**
     * Constructs a new DataSourceException with the specified detail message
     * and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new DataSourceException with the specified detail message
     * 
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     */
    public DataSourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DataSourceException with a cause.
     * 
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <tt>null</tt> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public DataSourceException(Throwable cause) {
        super(cause);
    }
}
